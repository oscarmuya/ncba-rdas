# RDAS Country Reference API

RDAS is a Spring Boot REST facade over the public CountryInfo SOAP service. It hides SOAP from consumers, hydrates country reference data into a local model, and serves read traffic from an in-memory Caffeine cache.

## Features

- REST API under `/api/v1`
- Country listing, search, filtering, sorting, and pagination
- Country, continent, currency, and language reference lookups
- Manual and scheduled cache refresh
- Stale-while-revalidate behavior when SOAP refresh fails
- JSON snapshot fallback for cold-start resilience
- Actuator health and Micrometer metrics
- Kubernetes deployment assets and operational guides

## Requirements

- Java 26
- Maven Wrapper from this repository
- Docker or another OCI image builder for container builds
- Kubernetes cluster access for deployment
- `envsubst`, usually provided by the `gettext` package, for manifest rendering

## Local Development

Run tests:

```bash
./mvnw test
```

Run the service:

```bash
./mvnw spring-boot:run
```

The API starts on the default Spring Boot port:

```text
http://localhost:8080
```

## Configuration

The main runtime settings are in `src/main/resources/application.yaml`.

| Property | Default | Purpose |
| --- | --- | --- |
| `rdas.soap.endpoint` | `https://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso` | CountryInfo SOAP endpoint |
| `rdas.soap.namespace-uri` | `http://www.oorsprong.org/websamples.countryinfo` | SOAP payload namespace |
| `rdas.cache.ttl` | `24h` | Freshness window for loaded country data |
| `rdas.cache.snapshot-path` | `./data/countries-snapshot.json` | Last-known-good snapshot file |
| `rdas.cache.stale-threshold` | `26h` | Health threshold for stale cache age |
| `rdas.refresh.startup-enabled` | `true` | Load data on application startup |
| `rdas.refresh.scheduled-enabled` | `true` | Enable scheduled cache refresh |
| `rdas.refresh.fixed-delay` | `24h` | Scheduled refresh interval |

## Documentation

- [API documentation](docs/api.md)
- [Architecture](docs/architecture.md)
- [Kubernetes deployment guide](docs/kubernetes-deployment.md)
- [Kubernetes troubleshooting guide](docs/kubernetes-troubleshooting.md)

## Container And Kubernetes

First cd into /rdas.

```
cd rdas
```

Build and push an image:

```bash
IMAGE=ghcr.io/oscarmuya/ncba-rdas/rdas:0.0.1 PUSH=false ./scripts/build-image.sh
```

Deploy to Kubernetes:

```bash
IMAGE=ghcr.io/oscarmuya/ncba-rdas/rdas:0.0.1 ./scripts/deploy-kubernetes.sh
```

Remove the deployment:

```bash
./scripts/undeploy-kubernetes.sh
```

## Health And Metrics

Actuator endpoints are exposed under `/actuator`.

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

Important metrics:

- `rdas.cache.age`
- `rdas.soap.errors`
- `rdas.soap.latency`

All `/api/v1` responses include `X-Data-Freshness` with one of:

- `fresh`
- `stale`
- `unavailable`
