#!/usr/bin/env bash
set -Eeuo pipefail

NAMESPACE="${K8S_NAMESPACE:-travel-platform}"
HEALTH_HOST="${HEALTH_HOST:-127.0.0.1}"
HEALTH_PORT="${HEALTH_PORT:-18080}"
BASE_URL="http://${HEALTH_HOST}:${HEALTH_PORT}"

command -v kubectl >/dev/null 2>&1 || {
  echo "kubectl is required." >&2
  exit 1
}
command -v curl >/dev/null 2>&1 || {
  echo "curl is required." >&2
  exit 1
}

kubectl --namespace "${NAMESPACE}" rollout status statefulset/mysql --timeout=60s
kubectl --namespace "${NAMESPACE}" rollout status deployment/backend --timeout=60s
kubectl --namespace "${NAMESPACE}" rollout status deployment/frontend --timeout=60s
kubectl --namespace "${NAMESPACE}" wait \
  --for=condition=Ready pod --all --timeout=60s

frontend_health="$(curl --fail --silent --show-error \
  --retry 20 --retry-delay 3 --retry-all-errors \
  "${BASE_URL}/healthz")"
if [[ "${frontend_health}" != "ok" ]]; then
  echo "Frontend health check returned an unexpected body: ${frontend_health}" >&2
  exit 1
fi

backend_health="$(curl --fail --silent --show-error \
  --retry 20 --retry-delay 3 --retry-all-errors \
  "${BASE_URL}/api/public/health")"
if [[ "${backend_health}" != *'"code":200'* || "${backend_health}" != *'"status":"UP"'* ]]; then
  echo "Backend health check returned an unexpected body: ${backend_health}" >&2
  exit 1
fi

curl --fail --silent --show-error \
  --retry 10 --retry-delay 2 --retry-all-errors \
  --output /dev/null "${BASE_URL}/"

echo "Frontend health: ${frontend_health}"
echo "Backend health: ${backend_health}"
echo "Deployed images:"
kubectl --namespace "${NAMESPACE}" get deployments \
  --output=jsonpath='{range .items[*]}{.metadata.name}{"="}{.spec.template.spec.containers[0].image}{"\n"}{end}'
echo "Kubernetes health checks passed."

