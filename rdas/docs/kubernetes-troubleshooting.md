# Kubernetes Troubleshooting Guide

Use this guide when RDAS is unhealthy, unavailable, stale, or returning unexpected API responses in Kubernetes.

## Quick Status

```bash
kubectl -n rdas get deploy,rs,pod,svc,pvc
kubectl -n rdas rollout status deployment/rdas
kubectl -n rdas get events --sort-by=.lastTimestamp
```

## Pods Are Not Starting

Check pod details:

```bash
kubectl -n rdas describe pod -l app.kubernetes.io/name=rdas
```

Check logs:

```bash
kubectl -n rdas logs deployment/rdas --tail=200
```

Common causes:

| Symptom | Likely Cause | Fix |
| --- | --- | --- |
| `ImagePullBackOff` | Image name, tag, or registry credentials are wrong | Confirm `IMAGE`, push the image, configure image pull secret |
| `CrashLoopBackOff` | App exits during startup | Check logs, config values, Java memory, SOAP connectivity |
| `CreateContainerConfigError` | Invalid ConfigMap or env reference | Re-render manifest and inspect environment values |
| Pod pending | PVC cannot bind or insufficient resources | Check storage class, node capacity, and resource requests |
| Multi-attach volume error | Multiple replicas are using a `ReadWriteOnce` PVC | Set `REPLICAS=1`, use `ReadWriteMany`, or move snapshots to object storage |

## Health Endpoint Is Down

Port-forward:

```bash
kubectl -n rdas port-forward svc/rdas 8080:80
curl -i http://localhost:8080/actuator/health
```

Health is down when:

- no country dataset exists
- cache age exceeds `RDAS_CACHE_STALE_THRESHOLD`
- dependent auto-configured health contributors fail

Inspect cache details in logs:

```bash
kubectl -n rdas logs deployment/rdas --tail=300 | grep -i 'refresh\|cache\|soap'
```

Trigger refresh:

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/refresh
```

## API Returns 503

`503` means no reference dataset is currently available.

Check:

```bash
kubectl -n rdas logs deployment/rdas --tail=300
kubectl -n rdas exec deploy/rdas -- ls -l /app/data
```

Likely causes:

- SOAP service is unavailable during cold start
- DNS or egress from the cluster to the SOAP endpoint is blocked
- snapshot file does not exist yet
- snapshot PVC is not mounted or not writable

Actions:

```bash
kubectl -n rdas exec deploy/rdas -- sh -c 'test -w /app/data && echo writable'
kubectl -n rdas exec deploy/rdas -- sh -c 'ls -lah /app/data'
kubectl -n rdas rollout restart deployment/rdas
```

If egress is blocked, ask the platform team to allow outbound HTTPS to:

```text
webservices.oorsprong.org:443
```

## API Returns Stale Data

Check the response header:

```bash
curl -i http://localhost:8080/api/v1/countries?size=1
```

If the header is:

```text
X-Data-Freshness: stale
```

RDAS is serving the latest-good dataset because the cache TTL has passed and refresh has not succeeded.

Check SOAP errors:

```bash
curl http://localhost:8080/actuator/metrics/rdas.soap.errors
curl http://localhost:8080/actuator/metrics/rdas.cache.age
```

Trigger refresh:

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/refresh
```

Then check:

```bash
curl -i http://localhost:8080/api/v1/countries?size=1
```

## SOAP Connectivity Problems

Check the configured endpoint:

```bash
kubectl -n rdas exec deploy/rdas -- printenv | grep RDAS_SOAP
```

Run a temporary network probe pod:

```bash
kubectl -n rdas run netcheck --rm -it --image=curlimages/curl --restart=Never -- \
  curl -I https://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso
```

If this fails:

- verify cluster DNS
- verify egress network policy
- verify proxy or firewall requirements
- verify TLS interception rules if your platform uses them

## Snapshot Problems

Check PVC:

```bash
kubectl -n rdas get pvc rdas-snapshot
kubectl -n rdas describe pvc rdas-snapshot
```

Check mounted files:

```bash
kubectl -n rdas exec deploy/rdas -- ls -lah /app/data
```

Expected file:

```text
/app/data/countries-snapshot.json
```

If the file is missing:

1. Trigger a manual refresh.
2. Confirm SOAP connectivity.
3. Confirm `/app/data` is writable.

## Rollout Is Stuck

Inspect rollout:

```bash
kubectl -n rdas rollout status deployment/rdas
kubectl -n rdas describe deployment rdas
kubectl -n rdas get rs
```

Undo the last rollout:

```bash
kubectl -n rdas rollout undo deployment/rdas
```

Restart without changing image:

```bash
kubectl -n rdas rollout restart deployment/rdas
```

## Ingress Does Not Work

Check service first:

```bash
kubectl -n rdas port-forward svc/rdas 8080:80
curl http://localhost:8080/api/v1/countries?size=1
```

Then check ingress:

```bash
kubectl -n rdas get ingress rdas
kubectl -n rdas describe ingress rdas
```

Common causes:

- `INGRESS_ENABLED` was not set to `true`
- wrong `INGRESS_CLASS`
- DNS does not point to the ingress load balancer
- TLS or host rules are configured elsewhere in the platform

## HPA Does Not Scale

Check metrics-server:

```bash
kubectl top nodes
kubectl -n rdas top pods
```

Check HPA:

```bash
kubectl -n rdas describe hpa rdas
```

Common causes:

- metrics-server is not installed
- CPU requests are missing
- current load is below threshold

## Useful Commands

```bash
kubectl -n rdas logs deployment/rdas --tail=300
kubectl -n rdas logs deployment/rdas -f
kubectl -n rdas exec deploy/rdas -- printenv | sort
kubectl -n rdas describe pod -l app.kubernetes.io/name=rdas
kubectl -n rdas get events --sort-by=.lastTimestamp
kubectl -n rdas rollout history deployment/rdas
kubectl -n rdas port-forward svc/rdas 8080:80
```

## Escalation Data To Capture

When escalating an incident, include:

- current image tag
- output of `kubectl -n rdas get deploy,pod,svc,pvc`
- last 300 application log lines
- `/actuator/health` response
- `X-Data-Freshness` header from a sample API response
- current `RDAS_SOAP_*` and `RDAS_CACHE_*` environment values with secrets removed
