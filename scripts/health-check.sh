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
for deployment in user-service product-service order-service content-trip-service gateway-service frontend; do
  kubectl --namespace "${NAMESPACE}" rollout status "deployment/${deployment}" --timeout=60s
done

frontend_health="$(curl --fail --silent --show-error \
  --retry 20 --retry-delay 3 --retry-all-errors \
  "${BASE_URL}/healthz")"
if [[ "${frontend_health}" != "ok" ]]; then
  echo "Frontend health check returned an unexpected body: ${frontend_health}" >&2
  exit 1
fi

gateway_health="$(curl --fail --silent --show-error \
  --retry 20 --retry-delay 3 --retry-all-errors \
  "${BASE_URL}/api/public/health")"
if [[ "${gateway_health}" != *'"code":200'* || "${gateway_health}" != *'"status":"UP"'* ]]; then
  echo "Gateway health check returned an unexpected body: ${gateway_health}" >&2
  exit 1
fi

for service in user-service:8101 product-service:8102 order-service:8103 content-trip-service:8104 gateway-service:8000; do
  service_name="${service%%:*}"
  service_port="${service##*:}"
  response="$(kubectl --namespace "${NAMESPACE}" exec \
    "deployment/${service_name}" -- \
    curl --fail --silent --show-error \
    "http://127.0.0.1:${service_port}/api/public/health")"

  if [[ "${response}" != *'"code":200'* || "${response}" != *'"status":"UP"'* ]]; then
    echo "${service_name} health check returned an unexpected body: ${response}" >&2
    exit 1
  fi
  echo "${service_name} health: ${response}"
done

curl --fail --silent --show-error \
  --retry 10 --retry-delay 2 --retry-all-errors \
  --output /dev/null "${BASE_URL}/"

echo "Frontend health: ${frontend_health}"
echo "Gateway health: ${gateway_health}"
echo "Deployed images:"
kubectl --namespace "${NAMESPACE}" get deployments \
  --output=jsonpath='{range .items[*]}{.metadata.name}{"="}{.spec.template.spec.containers[0].image}{"\n"}{end}'
echo "Microservice Kubernetes health checks passed."
