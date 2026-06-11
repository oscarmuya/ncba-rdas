#!/usr/bin/env sh
set -eu

MANIFEST="${MANIFEST:-target/kubernetes/rdas.yaml}"

if [ -f "$MANIFEST" ]; then
  kubectl delete -f "$MANIFEST" --ignore-not-found=true
else
  kubectl delete namespace "${NAMESPACE:-rdas}" --ignore-not-found=true
fi
