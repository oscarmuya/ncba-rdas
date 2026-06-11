# RDAS Architecture

## Overview

RDAS is an anti-corruption layer between REST consumers and the CountryInfo SOAP service.

```text
Mobile / Web / Partner / Internal Consumers
                  |
                  v
            RDAS REST API
                  |
                  v
          CountryService
                  |
                  v
        Caffeine CacheStore
                  |
                  v
        CacheRefreshService
                  |
                  v
        SoapCountryClient
                  |
                  v
      CountryInfo SOAP Service
```

Consumers never call SOAP directly. Read endpoints only query the local cache. SOAP is used during startup refresh, scheduled refresh, retry refresh, and manual admin refresh.

## Runtime Components

| Component | Responsibility |
| --- | --- |
| Controllers | Bind and validate HTTP requests, delegate to services |
| `CountryService` | Search, filter, sort, paginate, and map cached country data |
| `CacheStore` | Holds the latest dataset in Caffeine and keeps a latest-good reference |
| `CacheRefreshService` | Loads data from SOAP, updates cache atomically, schedules retries |
| `SnapshotService` | Writes and reads the JSON last-known-good snapshot |
| `SoapCountryClient` | Sole SOAP-aware component |
| `GlobalExceptionHandler` | Converts internal failures into stable API errors |
| `DataFreshnessHeaderAdvice` | Adds `X-Data-Freshness` to API responses |
| `CountryReferenceHealthIndicator` | Reports cache availability and stale threshold health |

## Data Model

RDAS hydrates a complete internal country dataset:

```text
CountryRecord
  isoCode
  name
  capital
  flagUrl
  phoneCode
  continent: ContinentRecord
  currency: CurrencyRecord
  languages: List<LanguageRecord>
```

The API exposes immutable DTO records derived from this internal model.

## Cache Strategy

- Cache implementation: Caffeine in-memory cache.
- TTL: 24 hours by default.
- Dataset granularity: one fully hydrated country dataset.
- Reads: all consumer reads are served from cache.
- Expiration behavior: if Caffeine expires the entry, `CacheStore` still serves the latest-good dataset as stale.
- Freshness header:
  - `fresh`: dataset exists and is within TTL
  - `stale`: dataset exists but is past TTL
  - `unavailable`: no dataset exists

## Refresh Strategy

Refresh triggers:

- Application startup
- Scheduled refresh every 24 hours
- Manual `POST /api/v1/admin/cache/refresh`
- Retry refresh after failure

Refresh behavior:

1. Load reference data from the SOAP service.
2. Build a new `CountryDataset`.
3. Atomically replace the cache.
4. Persist a snapshot to `rdas.cache.snapshot-path`.
5. Reset retry state.

On refresh failure:

- Existing cache remains live.
- Cold startup attempts to load the snapshot.
- Retry is scheduled with backoff: `1m`, `5m`, `15m`, `30m`.
- SOAP error details are not exposed to consumers.

## Resilience

| Scenario | Behavior |
| --- | --- |
| SOAP outage with warm cache | Serve cached data normally |
| SOAP outage with expired cache | Serve stale latest-good data |
| Cold start with SOAP outage and snapshot | Serve snapshot data |
| Cold start with SOAP outage and no snapshot | Return `503` |

## Observability

Actuator endpoints:

- `/actuator/health`
- `/actuator/info`
- `/actuator/metrics`

Metrics:

- `rdas.cache.age`: age of current dataset in seconds
- `rdas.soap.errors`: SOAP operation failure counter
- `rdas.soap.latency`: SOAP operation latency timer tagged by operation

Health behavior:

- Up when cache exists and age is below `rdas.cache.stale-threshold`
- Down when no cache exists or age exceeds stale threshold
