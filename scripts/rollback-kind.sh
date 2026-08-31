#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${K8S_NAMESPACE:-travel-platform}"
TARGET="${1:-all}"
REVISION="${2:-}"
ROLLBACK_TIMEOUT="${ROLLBACK_TIMEOUT:-300s}"
SKIP_HEALTH_CHECK="${SKIP_HEALTH_CHECK:-false}"
ARTIFACT_DIR="${ROLLBACK_ARTIFACT_DIR:-artifacts/rollback}"

ALL_DEPLOYMENTS=(
  user-service
  product-service
  order-service
  content-trip-service
  gateway-service
  frontend
)
BUSINESS_DEPLOYMENTS=(
  user-service
  product-service
  order-service
  content-trip-service
)

usage() {
  cat <<'EOF'
Usage:
  bash scripts/rollback-kind.sh [target] [revision]

Targets:
  all                   Roll back all four business services, gateway and frontend
  business              Roll back the four business services
  user-service          Roll back only user-service
  product-service       Roll back only product-service
  order-service         Roll back only order-service
  content-trip-service  Roll back only content-trip-service
  gateway-service       Roll back only gateway-service
  frontend              Roll back only frontend

Examples:
  bash scripts/rollback-kind.sh
  bash scripts/rollback-kind.sh business
  bash scripts/rollback-kind.sh gateway-service
  bash scripts/rollback-kind.sh order-service 3

Without a revision, each selected Deployment is rolled back to its immediately
previous Kubernetes revision. A specific revision is accepted only for one
Deployment, because revision numbers are local to each Deployment.

Optional environment variables:
  K8S_NAMESPACE          Namespace to use (default: travel-platform)
  ROLLBACK_TIMEOUT       kubectl rollout timeout (default: 300s)
  SKIP_HEALTH_CHECK      Set to true to skip scripts/health-check.sh
  ROLLBACK_ARTIFACT_DIR  Evidence directory (default: artifacts/rollback)
EOF
}

if [[ "${TARGET}" == "-h" || "${TARGET}" == "--help" ]]; then
  usage
  exit 0
fi

case "${TARGET}" in
  all)
    deployments=("${ALL_DEPLOYMENTS[@]}")
    ;;
  business)
    deployments=("${BUSINESS_DEPLOYMENTS[@]}")
    ;;
  user-service|product-service|order-service|content-trip-service|gateway-service|frontend)
    deployments=("${TARGET}")
    ;;
  *)
    echo "Unknown rollback target: ${TARGET}" >&2
    usage >&2
    exit 2
    ;;
esac

if [[ -n "${REVISION}" && ! "${REVISION}" =~ ^[1-9][0-9]*$ ]]; then
  echo "Revision must be a positive integer." >&2
  exit 2
fi

if [[ -n "${REVISION}" && "${#deployments[@]}" -ne 1 ]]; then
  echo "A revision can only be supplied when one Deployment is selected." >&2
  exit 2
fi

command -v kubectl >/dev/null 2>&1 || {
  echo "kubectl is required." >&2
  exit 1
}

if ! kubectl get namespace "${NAMESPACE}" >/dev/null 2>&1; then
  echo "Kubernetes namespace ${NAMESPACE} does not exist or is not reachable." >&2
  exit 1
fi

mkdir -p "${ARTIFACT_DIR}"

preflight_deployment() {
  local deployment="$1"
  local history_file="${ARTIFACT_DIR}/${deployment}-history-before.txt"

  if ! kubectl --namespace "${NAMESPACE}" get "deployment/${deployment}" >/dev/null 2>&1; then
    echo "Deployment ${deployment} does not exist in namespace ${NAMESPACE}." >&2
    return 1
  fi

  kubectl --namespace "${NAMESPACE}" rollout history "deployment/${deployment}" \
    | tee "${history_file}"

  if [[ -n "${REVISION}" ]]; then
    if ! kubectl --namespace "${NAMESPACE}" rollout history "deployment/${deployment}" \
      --revision="${REVISION}" >/dev/null 2>&1; then
      echo "Revision ${REVISION} does not exist for ${deployment}." >&2
      return 1
    fi
  else
    local revision_count
    revision_count="$(awk '/^[[:space:]]*[0-9]+[[:space:]]/ { count++ } END { print count+0 }' "${history_file}")"
    if (( revision_count < 2 )); then
      echo "Deployment ${deployment} has no previous revision to roll back to." >&2
      return 1
    fi
  fi
}

rollback_deployment() {
  local deployment="$1"
  local before_image
  local after_image
  local undo_args=("deployment/${deployment}")

  before_image="$(kubectl --namespace "${NAMESPACE}" get "deployment/${deployment}" \
    --output="jsonpath={.spec.template.spec.containers[?(@.name=='${deployment}')].image}")"

  if [[ -n "${REVISION}" ]]; then
    undo_args+=("--to-revision=${REVISION}")
    echo "Rolling back ${deployment} to revision ${REVISION}..."
  else
    echo "Rolling back ${deployment} to its previous revision..."
  fi

  kubectl --namespace "${NAMESPACE}" rollout undo "${undo_args[@]}"
  kubectl --namespace "${NAMESPACE}" rollout status "deployment/${deployment}" \
    --timeout="${ROLLBACK_TIMEOUT}"

  after_image="$(kubectl --namespace "${NAMESPACE}" get "deployment/${deployment}" \
    --output="jsonpath={.spec.template.spec.containers[?(@.name=='${deployment}')].image}")"

  {
    echo "deployment=${deployment}"
    echo "image_before=${before_image}"
    echo "image_after=${after_image}"
  } | tee "${ARTIFACT_DIR}/${deployment}-result.txt"

  kubectl --namespace "${NAMESPACE}" rollout history "deployment/${deployment}" \
    > "${ARTIFACT_DIR}/${deployment}-history-after.txt"
}

# Validate every selected target before mutating the cluster. This prevents an
# "all" rollback from stopping halfway because one service has no old revision.
for deployment in "${deployments[@]}"; do
  preflight_deployment "${deployment}"
done

for deployment in "${deployments[@]}"; do
  rollback_deployment "${deployment}"
done

kubectl --namespace "${NAMESPACE}" get deployments,pods -o wide \
  | tee "${ARTIFACT_DIR}/workloads-after.txt"

if [[ "${SKIP_HEALTH_CHECK}" == "true" ]]; then
  echo "Rollback completed. Health check skipped because SKIP_HEALTH_CHECK=true."
else
  echo "Running post-rollback health checks..."
  bash "${SCRIPT_DIR}/health-check.sh" | tee "${ARTIFACT_DIR}/health-check.txt"
fi

echo "Microservice Kubernetes rollback completed successfully."
