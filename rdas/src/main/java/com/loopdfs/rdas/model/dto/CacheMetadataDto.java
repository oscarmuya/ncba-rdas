package com.loopdfs.rdas.model.dto;

import java.time.Instant;

public record CacheMetadataDto(String status, String freshness, Instant refreshedAt, Instant expiresAt, long ageSeconds,
    int countryCount) {
}
