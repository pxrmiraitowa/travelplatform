#!/usr/bin/env bash
set -Eeuo pipefail

SCRIPT_DIR="$(cd "$(dirname "${BASH_SOURCE[0]}")" && pwd)"
NAMESPACE="${K8S_NAMESPACE:-travel-platform}"
TARGET="${1:-all}"
REVISION="${2:-}"
ROLLBACK_TIMEOUT="${ROLLBACK_TIMEOUT:-300s}"
SKIP_HEALTH_CHECK="${SKIP_HEALTH_CHECK:-false}"

usage() {
  cat <<'EOF'
Usage:
  bash scripts/rollback-kind.sh [all|backend|frontend] [revision]

Examples:
  bash scripts/rollback-kind.sh
  bash scripts/rollback-kind.sh backend
  bash scripts/rollback-kind.sh frontend
  bash scripts/rollback-kind.sh backend 3

The default target is "all" and the default behavior is to roll each selected
Deployment back to its immediately previous Kubernetes revision. A specific
revision can only be supplied when rolling back one Deployment.

Optional environment variables:
  K8S_NAMESPACE       Namespace to use (default: travel-platform)
  ROLLBACK_TIMEOUT    kubectl rollout timeout (default: 300s)
  SKIP_HEALTH_CHECK   Set to true to skip scripts/health-check.sh
EOF
}

if [[ "${TARGET}" == "-h" || "${TARGET}" == "--help" ]]; then
  usage
  exit 0
fi

case "${TARGET}" in
  all|backend|frontend)
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

if [[ "${TARGET}" == "all" && -n "${REVISION}" ]]; then
  echo "A revision cannot be shared safely by both Deployments." >&2
  echo "Rollback backend and frontend separately when selecting a revision." >&2
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

rollback_deployment() {
  local deployment="$1"
  local before_image
  local after_image
  local undo_args=("deployment/${deployment}")

  if ! kubectl --namespace "${NAMESPACE}" get "deployment/${deployment}" >/dev/null 2>&1; then
    echo "Deployment ${deployment} does not exist in namespace ${NAMESPACE}." >&2
    return 1
  fi

  before_image="$(kubectl --namespace "${NAMESPACE}" get "deployment/${deployment}" \
    --output=jsonpath='{.spec.template.spec.containers[0].image}')"

  echo "Rollback history for ${deployment}:"
  kubectl --namespace "${NAMESPACE}" rollout history "deployment/${deployment}"

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
    --output=jsonpath='{.spec.template.spec.containers[0].image}')"

  echo "${deployment} image before rollback: ${before_image}"
  echo "${deployment} image after rollback:  ${after_image}"
}

case "${TARGET}" in
  backend)
    rollback_deployment backend
    ;;
  frontend)
    rollback_deployment frontend
    ;;
  all)
    rollback_deployment backend
    rollback_deployment frontend
    ;;
esac

kubectl --namespace "${NAMESPACE}" get deployments,pods -o wide

if [[ "${SKIP_HEALTH_CHECK}" == "true" ]]; then
  echo "Rollback completed. Health check skipped because SKIP_HEALTH_CHECK=true."
else
  echo "Running post-rollback health checks..."
  bash "${SCRIPT_DIR}/health-check.sh"
fi

echo "Kubernetes rollback completed successfully."
