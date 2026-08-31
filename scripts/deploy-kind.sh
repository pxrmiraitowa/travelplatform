#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
NAMESPACE="${K8S_NAMESPACE:-travel-platform}"

: "${BACKEND_IMAGE:?BACKEND_IMAGE is required, for example ghcr.io/owner/travel-platform-server}"
: "${FRONTEND_IMAGE:?FRONTEND_IMAGE is required, for example ghcr.io/owner/travel-platform-web}"
: "${IMAGE_TAG:?IMAGE_TAG is required and must not be latest}"
: "${MYSQL_ROOT_PASSWORD:?MYSQL_ROOT_PASSWORD is required}"
: "${JWT_SECRET:?JWT_SECRET is required}"

if [[ "${IMAGE_TAG}" == "latest" ]]; then
  echo "IMAGE_TAG must be a versioned tag; latest is not allowed." >&2
  exit 1
fi

command -v kubectl >/dev/null 2>&1 || {
  echo "kubectl is required." >&2
  exit 1
}

apply_manifest_with_image() {
  local manifest="$1"
  local placeholder_image="$2"
  local deployment_image="$3"

  # Render the real versioned image before the Deployment is first created.
  # Applying the placeholder and updating it afterwards briefly creates an
  # ImagePullBackOff Pod that can interfere with deployment health checks.
  if ! grep -Fq "image: ${placeholder_image}" "${manifest}"; then
    echo "Expected placeholder image ${placeholder_image} was not found in ${manifest}." >&2
    exit 1
  fi

  sed "s|image: ${placeholder_image}|image: ${deployment_image}|" "${manifest}" \
    | kubectl apply -f -
}

kubectl apply -f "${REPO_ROOT}/deploy/k8s/namespace.yaml"

kubectl --namespace "${NAMESPACE}" create secret generic travel-platform-secrets \
  --from-literal=mysql-root-password="${MYSQL_ROOT_PASSWORD}" \
  --from-literal=jwt-secret="${JWT_SECRET}" \
  --dry-run=client \
  --output=yaml | kubectl apply -f -

kubectl --namespace "${NAMESPACE}" create configmap travel-platform-db-init \
  --from-file=01-schema.sql="${REPO_ROOT}/travel-platform-server/src/main/resources/sql/schema.sql" \
  --from-file=02-demo-data.sql="${REPO_ROOT}/travel-platform-server/src/main/resources/sql/data-demo.sql" \
  --from-file=03-demo-data-migration-20260601.sql="${REPO_ROOT}/travel-platform-server/scripts/demo-data-patch-20260601.sql" \
  --from-file=04-demo-data-charset-repair-20260826.sql="${REPO_ROOT}/travel-platform-server/scripts/demo-data-charset-repair-20260826.sql" \
  --dry-run=client \
  --output=yaml | kubectl apply -f -

kubectl apply -f "${REPO_ROOT}/deploy/k8s/mysql-storage.yaml"
kubectl apply -f "${REPO_ROOT}/deploy/k8s/mysql.yaml"

echo "Waiting for MySQL and database initialization..."
kubectl --namespace "${NAMESPACE}" rollout status statefulset/mysql --timeout=240s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/backend.yaml" \
  "travel-platform-server:0.1.0" \
  "${BACKEND_IMAGE}:${IMAGE_TAG}"

echo "Waiting for backend rollout..."
kubectl --namespace "${NAMESPACE}" rollout status deployment/backend --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/frontend.yaml" \
  "travel-platform-web:0.1.0" \
  "${FRONTEND_IMAGE}:${IMAGE_TAG}"

echo "Waiting for frontend rollout..."
kubectl --namespace "${NAMESPACE}" rollout status deployment/frontend --timeout=180s

kubectl --namespace "${NAMESPACE}" get pods,services
