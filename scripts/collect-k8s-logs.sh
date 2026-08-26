#!/usr/bin/env bash
set +e

OUTPUT_DIR="${1:-artifacts/k8s}"
NAMESPACE="${K8S_NAMESPACE:-travel-platform}"

mkdir -p "${OUTPUT_DIR}"

if ! command -v kubectl >/dev/null 2>&1; then
  echo "kubectl is not installed; no Kubernetes diagnostics are available." \
    > "${OUTPUT_DIR}/not-available.txt"
  exit 0
fi

if ! kubectl cluster-info > "${OUTPUT_DIR}/cluster-info.txt" 2>&1; then
  echo "The Kubernetes cluster was not created or is not reachable." \
    > "${OUTPUT_DIR}/not-available.txt"
  exit 0
fi

kubectl get nodes -o wide > "${OUTPUT_DIR}/nodes.txt" 2>&1
kubectl --namespace "${NAMESPACE}" get all,pvc -o wide \
  > "${OUTPUT_DIR}/resources.txt" 2>&1
kubectl --namespace "${NAMESPACE}" get events --sort-by=.metadata.creationTimestamp \
  > "${OUTPUT_DIR}/events.txt" 2>&1
kubectl --namespace "${NAMESPACE}" describe pods \
  > "${OUTPUT_DIR}/pods-describe.txt" 2>&1

for pod in $(kubectl --namespace "${NAMESPACE}" get pods -o name 2>/dev/null); do
  safe_name="${pod//\//-}"
  kubectl --namespace "${NAMESPACE}" logs "${pod}" --all-containers --timestamps \
    > "${OUTPUT_DIR}/${safe_name}.log" 2>&1
  kubectl --namespace "${NAMESPACE}" logs "${pod}" --all-containers --timestamps --previous \
    > "${OUTPUT_DIR}/${safe_name}-previous.log" 2>&1
done

exit 0
