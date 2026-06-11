# RDAS API Documentation

Base URL:

```text
/api/v1
```

All API responses include:

```text
X-Data-Freshness: fresh | stale | unavailable
```

## Country Object

```json
{
  "isoCode": "KE",
  "name": "Kenya",
  "capital": "Nairobi",
  "flagUrl": "https://example.test/ke.gif",
  "phoneCode": "254",
  "continent": {
    "code": "AF",
    "name": "Africa"
  },
  "currency": {
    "code": "KES",
    "name": "Kenyan Shilling"
  },
  "languages": [
    {
      "code": "SWA",
      "name": "Swahili"
    }
  ]
}
```

## List And Search Countries

```http
GET /api/v1/countries
```

Query parameters:

| Name | Type | Default | Description |
| --- | --- | --- | --- |
| `search` | string | none | Partial, case-insensitive country name match |
| `continent` | string | none | Continent code, for example `AF` |
| `currency` | string | none | Currency code, for example `KES` |
| `language` | string | none | Language code, for example `SW` |
| `page` | integer | `0` | Zero-based page number |
| `size` | integer | `20` | Page size, max `250` |
| `sort` | string | `name,asc` | Sort field and direction |

Supported sort fields:

- `name`
- `isoCode`
- `capital`
- `continent`
- `currency`

Example:

```bash
curl 'http://localhost:8080/api/v1/countries?search=ken&continent=AF&currency=KES&language=SWA&page=0&size=20&sort=name,asc'
```

Response:

```json
{
  "content": [
    {
      "isoCode": "KE",
      "name": "Kenya",
      "capital": "Nairobi",
      "flagUrl": "http://www.oorsprong.org/WebSamples.CountryInfo/Flags/Kenya.jpg",
      "phoneCode": "254",
      "continent": {
        "code": "AF",
        "name": "Africa"
      },
      "currency": {
        "code": "KES",
        "name": "Shillings"
      },
      "languages": [
        {
          "code": "SWA",
          "name": "Swahili"
        }
      ]
    }
  ],
  "page": 0,
  "size": 20,
  "totalElements": 1,
  "totalPages": 1
}
```

## Get Single Country

```http
GET /api/v1/countries/{isoCode}
```

Example:

```bash
curl http://localhost:8080/api/v1/countries/KE
```

Status codes:

- `200` when the country exists
- `404` when the ISO code is unknown
- `503` when no reference data is available

## Countries Sharing A Currency

```http
GET /api/v1/currencies/{currencyCode}/countries
```

Query parameters:

| Name | Type | Default |
| --- | --- | --- |
| `page` | integer | `0` |
| `size` | integer | `20` |
| `sort` | string | `name,asc` |

Example:

```bash
curl 'http://localhost:8080/api/v1/currencies/EUR/countries?sort=name,asc'
```

## Reference Lookups

```http
GET /api/v1/continents
GET /api/v1/currencies
GET /api/v1/languages
```

Example response:

```json
[
  {
    "code": "AF",
    "name": "Africa"
  }
]
```

## Admin Cache Refresh

```http
POST /api/v1/admin/cache/refresh
```

Triggers a manual refresh from the SOAP service. The existing cache remains available if refresh fails.

Example:

```bash
curl -X POST http://localhost:8080/api/v1/admin/cache/refresh
```

Response:

```json
{
  "status": "refreshed",
  "freshness": "fresh",
  "refreshedAt": "2026-06-11T12:00:00Z",
  "expiresAt": "2026-06-12T12:00:00Z",
  "ageSeconds": 0,
  "countryCount": 195
}
```

## Error Response

```json
{
  "timestamp": "2026-06-11T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Unsupported sort value: population,asc",
  "path": "/api/v1/countries"
}
```

Common status codes:

| Status | Meaning |
| --- | --- |
| `400` | Invalid request parameter or unsupported sort |
| `404` | Country not found |
| `503` | Reference data unavailable |
| `500` | Unexpected service error |
