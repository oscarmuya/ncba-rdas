#!/usr/bin/env sh
set -eu

export NAMESPACE="${NAMESPACE:-rdas}"
export IMAGE="${IMAGE:-ghcr.io/oscarmuya/ncba-rdas/rdas:latest}"
export IMAGE_PULL_POLICY="${IMAGE_PULL_POLICY:-IfNotPresent}"
export REPLICAS="${REPLICAS:-1}"

export RDAS_SOAP_ENDPOINT="${RDAS_SOAP_ENDPOINT:-http://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso}"
export RDAS_SOAP_NAMESPACE_URI="${RDAS_SOAP_NAMESPACE_URI:-http://www.oorsprong.org/websamples.countryinfo}"
export RDAS_SOAP_CONNECT_TIMEOUT="${RDAS_SOAP_CONNECT_TIMEOUT:-5s}"
export RDAS_SOAP_READ_TIMEOUT="${RDAS_SOAP_READ_TIMEOUT:-20s}"
export RDAS_CACHE_TTL="${RDAS_CACHE_TTL:-24h}"
export RDAS_CACHE_SNAPSHOT_PATH="${RDAS_CACHE_SNAPSHOT_PATH:-/app/data/countries-snapshot.json}"
export RDAS_CACHE_STALE_THRESHOLD="${RDAS_CACHE_STALE_THRESHOLD:-26h}"
export RDAS_REFRESH_STARTUP_ENABLED="${RDAS_REFRESH_STARTUP_ENABLED:-true}"
export RDAS_REFRESH_SCHEDULED_ENABLED="${RDAS_REFRESH_SCHEDULED_ENABLED:-true}"
export RDAS_REFRESH_FIXED_DELAY="${RDAS_REFRESH_FIXED_DELAY:-24h}"

export SNAPSHOT_STORAGE_SIZE="${SNAPSHOT_STORAGE_SIZE:-1Gi}"
export SNAPSHOT_ACCESS_MODE="${SNAPSHOT_ACCESS_MODE:-ReadWriteOnce}"
export JAVA_TOOL_OPTIONS="${JAVA_TOOL_OPTIONS:--XX:MaxRAMPercentage=75 -XX:+ExitOnOutOfMemoryError}"
export CPU_REQUEST="${CPU_REQUEST:-100m}"
export MEMORY_REQUEST="${MEMORY_REQUEST:-256Mi}"
export CPU_LIMIT="${CPU_LIMIT:-1000m}"
export MEMORY_LIMIT="${MEMORY_LIMIT:-768Mi}"

export INGRESS_ENABLED="${INGRESS_ENABLED:-false}"
export INGRESS_CLASS="${INGRESS_CLASS:-nginx}"
export INGRESS_HOST="${INGRESS_HOST:-rdas.example.com}"

export HPA_ENABLED="${HPA_ENABLED:-false}"
export HPA_MIN_REPLICAS="${HPA_MIN_REPLICAS:-2}"
export HPA_MAX_REPLICAS="${HPA_MAX_REPLICAS:-5}"
export HPA_CPU_UTILIZATION="${HPA_CPU_UTILIZATION:-70}"

render() {
  envsubst < "$1"
  printf -- '\n---\n'
}

render deploy/kubernetes/namespace.yaml
render deploy/kubernetes/configmap.yaml
render deploy/kubernetes/pvc.yaml
render deploy/kubernetes/deployment.yaml
render deploy/kubernetes/service.yaml

if [ "$INGRESS_ENABLED" = "true" ]; then
  render deploy/kubernetes/ingress.yaml
fi

if [ "$HPA_ENABLED" = "true" ]; then
  render deploy/kubernetes/hpa.yaml
fi
