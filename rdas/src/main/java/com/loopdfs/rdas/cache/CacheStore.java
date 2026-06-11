package com.loopdfs.rdas.cache;

import java.time.Clock;
import java.util.Optional;
import java.util.concurrent.atomic.AtomicReference;

import com.github.benmanes.caffeine.cache.Cache;
import com.github.benmanes.caffeine.cache.Caffeine;
import com.loopdfs.rdas.config.RdasProperties;
import com.loopdfs.rdas.model.internal.CountryDataset;

import org.springframework.stereotype.Component;

@Component
public class CacheStore {

  private static final String DATASET_KEY = "countries";

  private final Cache<String, CountryDataset> cache;
  private final AtomicReference<CountryDataset> latestGoodDataset = new AtomicReference<>();
  private final Clock clock;

  public CacheStore(RdasProperties properties, Clock clock) {
    this.cache = Caffeine.newBuilder().expireAfterWrite(properties.cache().ttl()).build();
    this.clock = clock;
  }

  public Optional<CountryDataset> currentDataset() {
    CountryDataset cached = cache.getIfPresent(DATASET_KEY);
    if (cached != null) {
      return Optional.of(cached);
    }
    return Optional.ofNullable(latestGoodDataset.get());
  }

  public CountryDataset requireDataset() {
    return currentDataset().orElseThrow(() -> new com.loopdfs.rdas.exception.ReferenceDataUnavailableException(
        "Reference data temporarily unavailable. Please retry shortly."));
  }

  public void replace(CountryDataset dataset) {
    latestGoodDataset.set(dataset);
    cache.put(DATASET_KEY, dataset);
  }

  public DataFreshness freshness() {
    return currentDataset().map(dataset -> dataset.isExpired(clock) ? DataFreshness.STALE : DataFreshness.FRESH)
        .orElse(DataFreshness.UNAVAILABLE);
  }

  public CacheMetadata metadata() {
    return currentDataset()
        .map(dataset -> new CacheMetadata(freshness(), dataset.refreshedAt(), dataset.expiresAt(),
            dataset.ageSeconds(clock),
            dataset.countries().size()))
        .orElse(new CacheMetadata(DataFreshness.UNAVAILABLE, null, null, -1, 0));
  }
}
