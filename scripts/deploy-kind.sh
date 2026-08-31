#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
REPO_ROOT="$(cd "${SCRIPT_DIR}/.." && pwd)"
NAMESPACE="${K8S_NAMESPACE:-travel-platform}"

: "${IMAGE_PREFIX:?IMAGE_PREFIX is required, for example ghcr.io/owner/travel-platform}"
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

  if ! grep -Fq "image: ${placeholder_image}" "${manifest}"; then
    echo "Expected placeholder image ${placeholder_image} was not found in ${manifest}." >&2
    exit 1
  fi

  # Apply the real versioned image in one operation so the placeholder never
  # becomes a Kubernetes rollout revision. This keeps rollback history usable.
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
  --from-file=SqlRunner.java="${REPO_ROOT}/travel-platform-microservices/tools/SqlRunner.java" \
  --from-file=schema.sql="${REPO_ROOT}/travel-platform-server/src/main/resources/sql/schema.sql" \
  --from-file=data-demo.sql="${REPO_ROOT}/travel-platform-server/src/main/resources/sql/data-demo.sql" \
  --dry-run=client \
  --output=yaml | kubectl apply -f -

kubectl apply -f "${REPO_ROOT}/deploy/k8s/mysql.yaml"

echo "Waiting for MySQL..."
kubectl --namespace "${NAMESPACE}" rollout status statefulset/mysql --timeout=240s

kubectl --namespace "${NAMESPACE}" delete job db-init --ignore-not-found=true
kubectl apply -f "${REPO_ROOT}/deploy/k8s/db-init-job.yaml"

echo "Waiting for database initialization..."
kubectl --namespace "${NAMESPACE}" wait \
  --for=condition=complete job/db-init \
  --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/user-service.yaml" \
  "travel-platform-user-service:0.1.0" \
  "${IMAGE_PREFIX}-user-service:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/user-service --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/product-service.yaml" \
  "travel-platform-product-service:0.1.0" \
  "${IMAGE_PREFIX}-product-service:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/product-service --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/order-service.yaml" \
  "travel-platform-order-service:0.1.0" \
  "${IMAGE_PREFIX}-order-service:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/order-service --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/content-trip-service.yaml" \
  "travel-platform-content-trip-service:0.1.0" \
  "${IMAGE_PREFIX}-content-trip-service:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/content-trip-service --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/gateway-service.yaml" \
  "travel-platform-gateway-service:0.1.0" \
  "${IMAGE_PREFIX}-gateway-service:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/gateway-service --timeout=300s

apply_manifest_with_image \
  "${REPO_ROOT}/deploy/k8s/frontend.yaml" \
  "travel-platform-web:0.1.0" \
  "${IMAGE_PREFIX}-web:${IMAGE_TAG}"
kubectl --namespace "${NAMESPACE}" rollout status deployment/frontend --timeout=180s

kubectl --namespace "${NAMESPACE}" get pods,services
