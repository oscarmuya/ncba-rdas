package com.loopdfs.rdas.cache;

import java.time.Instant;

public record CacheMetadata(DataFreshness freshness, Instant refreshedAt, Instant expiresAt, long ageSeconds,
    int countryCount) {
}
