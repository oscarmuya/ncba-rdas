#!/usr/bin/env sh
set -eu

MANIFEST="${MANIFEST:-target/kubernetes/rdas.yaml}"
NAMESPACE="${NAMESPACE:-rdas}"

if ! command -v kubectl >/dev/null 2>&1; then
  printf 'error: kubectl is not installed or not in PATH\n' >&2
  exit 1
fi

if ! kubectl config current-context >/dev/null 2>&1; then
  printf 'error: kubectl has no active context. Set KUBECONFIG or run kubectl config use-context <context>.\n' >&2
  exit 1
fi

mkdir -p "$(dirname "$MANIFEST")"
./scripts/render-kubernetes.sh > "$MANIFEST"

kubectl apply -f "$MANIFEST"
kubectl -n "$NAMESPACE" rollout status deployment/rdas

printf 'Applied manifest: %s\n' "$MANIFEST"
