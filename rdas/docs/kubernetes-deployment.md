# Kubernetes Deployment Guide

This guide deploys RDAS to Kubernetes using the manifests and scripts in this repository.

## Prerequisites

- A Kubernetes cluster
- `kubectl` configured for the target cluster
- Docker or a compatible image builder
- Access to a container registry that the cluster can pull from
- Optional: ingress controller if public HTTP ingress is required
- Optional: metrics-server if HPA is enabled

## 1. Build And Test

```bash
./mvnw test
```

## 2. Build And Push The Container Image

Set the image name for your registry:

```bash
export IMAGE=ghcr.io/oscarmuya/ncba-rdas/rdas:0.0.1
```

Build:

```bash
./scripts/build-image.sh
```

Build and push:

```bash
PUSH=true ./scripts/build-image.sh
```

For multi-architecture builds, pass `PLATFORM`:

```bash
PLATFORM=linux/amd64 PUSH=true ./scripts/build-image.sh
```

## 3. Configure Deployment Values

Minimum required values:

```bash
export NAMESPACE=rdas
export IMAGE=ghcr.io/oscarmuya/ncba-rdas/rdas:0.0.1
export IMAGE_PULL_POLICY=IfNotPresent
```

Optional runtime values:

```bash
export REPLICAS=1
export RDAS_SOAP_ENDPOINT=https://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
export RDAS_SOAP_NAMESPACE_URI=http://www.oorsprong.org/websamples.countryinfo
export RDAS_SOAP_CONNECT_TIMEOUT=5s
export RDAS_SOAP_READ_TIMEOUT=20s
export RDAS_CACHE_TTL=24h
export RDAS_CACHE_SNAPSHOT_PATH=/app/data/countries-snapshot.json
export RDAS_CACHE_STALE_THRESHOLD=26h
export RDAS_REFRESH_STARTUP_ENABLED=true
export RDAS_REFRESH_SCHEDULED_ENABLED=true
export RDAS_REFRESH_FIXED_DELAY=24h
```

Resource and storage values:

```bash
export CPU_REQUEST=100m
export MEMORY_REQUEST=256Mi
export CPU_LIMIT=1000m
export MEMORY_LIMIT=768Mi
export SNAPSHOT_STORAGE_SIZE=1Gi
export SNAPSHOT_ACCESS_MODE=ReadWriteOnce
```

## 4. Render And Inspect Manifests

```bash
./scripts/render-kubernetes.sh > target/kubernetes/rdas.yaml
less target/kubernetes/rdas.yaml
```

The rendered manifest includes:

- Namespace
- ConfigMap
- PersistentVolumeClaim
- Deployment
- Service
- Optional Ingress
- Optional HPA

## 5. Deploy

```bash
./scripts/deploy-kubernetes.sh
```

The script renders `target/kubernetes/rdas.yaml`, applies it, and waits for the rollout.

Manual equivalent:

```bash
kubectl apply -f target/kubernetes/rdas.yaml
kubectl -n rdas rollout status deployment/rdas
```

## 6. Verify The Deployment

Check pods:

```bash
kubectl -n rdas get pods -l app.kubernetes.io/name=rdas
```

Check service:

```bash
kubectl -n rdas get svc rdas
```

Check health:

```bash
kubectl -n rdas port-forward svc/rdas 8080:80
curl http://localhost:8080/actuator/health
```

Check API:

```bash
curl http://localhost:8080/api/v1/countries?size=5
curl http://localhost:8080/api/v1/continents
```

Trigger manual cache refresh:

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/refresh
```

## 7. Enable Ingress

Set these values before deployment:

```bash
export INGRESS_ENABLED=true
export INGRESS_CLASS=nginx
export INGRESS_HOST=rdas.example.com
```

Deploy:

```bash
./scripts/deploy-kubernetes.sh
```

Verify:

```bash
kubectl -n rdas get ingress rdas
curl http://rdas.example.com/api/v1/countries?size=5
```

## 8. Enable Autoscaling

The cluster must have metrics-server installed.

```bash
export HPA_ENABLED=true
export HPA_MIN_REPLICAS=2
export HPA_MAX_REPLICAS=5
export HPA_CPU_UTILIZATION=70
./scripts/deploy-kubernetes.sh
```

Verify:

```bash
kubectl -n rdas get hpa rdas
```

## 9. Update The Application

Build and push a new image:

```bash
export IMAGE=registry.example.com/rdas:0.0.2
PUSH=true ./scripts/build-image.sh
```

Deploy:

```bash
./scripts/deploy-kubernetes.sh
```

Watch rollout:

```bash
kubectl -n rdas rollout status deployment/rdas
```

Rollback if needed:

```bash
kubectl -n rdas rollout undo deployment/rdas
```

## 10. Remove The Deployment

```bash
./scripts/undeploy-kubernetes.sh
```

If the rendered manifest is unavailable, the script deletes the namespace:

```bash
NAMESPACE=rdas ./scripts/undeploy-kubernetes.sh
```

