#!/usr/bin/env bash
set -Eeuo pipefail

NAMESPACE="${K8S_NAMESPACE:-travel-platform}"
DEPLOYMENT="${FAILURE_DRILL_DEPLOYMENT:-gateway-service}"
CONTAINER="${FAILURE_DRILL_CONTAINER:-gateway-service}"
ARTIFACT_DIR="${FAILURE_DRILL_ARTIFACT_DIR:-artifacts/failure-drill}"
BAD_IMAGE="invalid.local/travel-platform/${CONTAINER}:missing-${GITHUB_RUN_ID:-local}"

mkdir -p "${ARTIFACT_DIR}"
original_image="$(kubectl --namespace "${NAMESPACE}" get "deployment/${DEPLOYMENT}" \
  --output="jsonpath={.spec.template.spec.containers[?(@.name=='${CONTAINER}')].image}")"

restore() {
  kubectl --namespace "${NAMESPACE}" set image "deployment/${DEPLOYMENT}" \
    "${CONTAINER}=${original_image}" >/dev/null
  kubectl --namespace "${NAMESPACE}" rollout status "deployment/${DEPLOYMENT}" --timeout=180s
}
trap restore EXIT

echo "Injecting unavailable image ${BAD_IMAGE} into ${DEPLOYMENT}."
kubectl --namespace "${NAMESPACE}" set image "deployment/${DEPLOYMENT}" \
  "${CONTAINER}=${BAD_IMAGE}" >/dev/null

if kubectl --namespace "${NAMESPACE}" rollout status "deployment/${DEPLOYMENT}" --timeout=45s; then
  echo "Failure drill did not produce the expected rollout failure." >&2
  exit 1
fi

kubectl --namespace "${NAMESPACE}" describe "deployment/${DEPLOYMENT}" \
  > "${ARTIFACT_DIR}/deployment-describe.txt"
kubectl --namespace "${NAMESPACE}" get pods -o wide \
  > "${ARTIFACT_DIR}/pods.txt"
kubectl --namespace "${NAMESPACE}" get events --sort-by=.lastTimestamp \
  > "${ARTIFACT_DIR}/events.txt"

if ! grep -Eiq "(errimagepull|imagepullbackoff|failed to pull|pulling image)" \
  "${ARTIFACT_DIR}/deployment-describe.txt" "${ARTIFACT_DIR}/events.txt" >/dev/null; then
  echo "Expected image-pull diagnosis was not found in Kubernetes diagnostics." >&2
  exit 1
fi

echo "Expected image pull failure diagnosed. The original image will now be restored."
