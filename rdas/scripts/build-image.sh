#!/usr/bin/env sh
set -eu

IMAGE="${IMAGE:-rdas:local}"
PLATFORM="${PLATFORM:-}"
PUSH="${PUSH:-false}"

if [ -n "$PLATFORM" ]; then
  docker build --platform "$PLATFORM" -t "$IMAGE" .
else
  docker build -t "$IMAGE" .
fi

if [ "$PUSH" = "true" ]; then
  docker push "$IMAGE"
fi

printf 'Built image: %s\n' "$IMAGE"
