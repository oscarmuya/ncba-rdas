#!/usr/bin/env sh
set -eu

MANIFEST="${MANIFEST:-target/kubernetes/rdas.yaml}"

mkdir -p "$(dirname "$MANIFEST")"
./scripts/render-kubernetes.sh > "$MANIFEST"

kubectl apply -f "$MANIFEST"
kubectl -n "${NAMESPACE:-rdas}" rollout status deployment/rdas

printf 'Applied manifest: %s\n' "$MANIFEST"
