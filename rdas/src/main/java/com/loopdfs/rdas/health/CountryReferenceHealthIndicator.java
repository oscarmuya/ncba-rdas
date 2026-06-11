package com.loopdfs.rdas.health;

import java.time.Clock;

import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.config.RdasProperties;

import org.springframework.boot.health.contributor.Health;
import org.springframework.boot.health.contributor.HealthIndicator;
import org.springframework.stereotype.Component;

@Component
public class CountryReferenceHealthIndicator implements HealthIndicator {

  private final CacheStore cacheStore;
  private final RdasProperties properties;
  private final Clock clock;

  public CountryReferenceHealthIndicator(CacheStore cacheStore, RdasProperties properties, Clock clock) {
    this.cacheStore = cacheStore;
    this.properties = properties;
    this.clock = clock;
  }

  @Override
  public Health health() {
    return cacheStore.currentDataset().map(dataset -> {
      long ageSeconds = dataset.ageSeconds(clock);
      Health.Builder builder = ageSeconds > properties.cache().staleThreshold().toSeconds() ? Health.down()
          : Health.up();
      return builder.withDetail("freshness", cacheStore.freshness().headerValue())
          .withDetail("ageSeconds", ageSeconds)
          .withDetail("countryCount", dataset.countries().size())
          .withDetail("refreshedAt", dataset.refreshedAt())
          .build();
    }).orElseGet(() -> Health.down().withDetail("freshness", "unavailable").build());
  }
}
