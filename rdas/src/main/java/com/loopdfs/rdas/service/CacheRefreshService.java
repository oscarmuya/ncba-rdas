package com.loopdfs.rdas.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.List;
import java.util.concurrent.atomic.AtomicInteger;

import com.loopdfs.rdas.cache.CacheMetadata;
import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.client.SoapCountryClient;
import com.loopdfs.rdas.client.SoapCountryClient.CountryReferenceData;
import com.loopdfs.rdas.config.RdasProperties;
import com.loopdfs.rdas.model.internal.CountryDataset;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.boot.context.event.ApplicationReadyEvent;
import org.springframework.context.event.EventListener;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;

@Service
public class CacheRefreshService {

  private static final Logger log = LoggerFactory.getLogger(CacheRefreshService.class);
  private static final List<Duration> RETRY_DELAYS = List.of(Duration.ofMinutes(1), Duration.ofMinutes(5),
      Duration.ofMinutes(15), Duration.ofMinutes(30));

  private final SoapCountryClient soapCountryClient;
  private final CacheStore cacheStore;
  private final SnapshotService snapshotService;
  private final RdasProperties properties;
  private final Clock clock;
  private final TaskScheduler taskScheduler;
  private final AtomicInteger retryAttempt = new AtomicInteger();

  public CacheRefreshService(SoapCountryClient soapCountryClient, CacheStore cacheStore,
      SnapshotService snapshotService,
      RdasProperties properties, Clock clock, TaskScheduler taskScheduler) {
    this.soapCountryClient = soapCountryClient;
    this.cacheStore = cacheStore;
    this.snapshotService = snapshotService;
    this.properties = properties;
    this.clock = clock;
    this.taskScheduler = taskScheduler;
  }

  @EventListener(ApplicationReadyEvent.class)
  public void refreshOnStartup() {
    if (properties.refresh().startupEnabled()) {
      refreshWithFallback("startup");
    }
  }

  @Scheduled(fixedDelayString = "#{@refreshFixedDelayMillis}")
  public void scheduledRefresh() {
    if (properties.refresh().scheduledEnabled()) {
      refreshWithFallback("scheduled");
    }
  }

  public RefreshResult refreshNow() {
    return refreshWithFallback("manual");
  }

  public RefreshResult refreshWithFallback(String trigger) {
    try {
      CountryReferenceData referenceData = soapCountryClient.fetchReferenceData();
      CountryDataset dataset = CountryDataset.create(referenceData.countries(), referenceData.continents(),
          referenceData.currencies(), referenceData.languages(), properties.cache().ttl(), clock);
      cacheStore.replace(dataset);
      snapshotService.write(dataset);
      retryAttempt.set(0);
      return new RefreshResult(true, trigger, cacheStore.metadata(), null);
    } catch (RuntimeException ex) {
      log.warn("Country reference refresh failed for trigger={}", trigger);
      loadSnapshotIfCold();
      scheduleRetry();
      return new RefreshResult(false, trigger, cacheStore.metadata(), "Reference data refresh failed");
    }
  }

  private void loadSnapshotIfCold() {
    if (cacheStore.currentDataset().isEmpty()) {
      snapshotService.read().ifPresent(cacheStore::replace);
    }
  }

  private void scheduleRetry() {
    int attempt = retryAttempt.getAndUpdate(current -> Math.min(current + 1, RETRY_DELAYS.size() - 1));
    Duration delay = RETRY_DELAYS.get(Math.min(attempt, RETRY_DELAYS.size() - 1));
    taskScheduler.schedule(() -> refreshWithFallback("retry"), Instant.now(clock).plus(delay));
  }

  public record RefreshResult(boolean success, String trigger, CacheMetadata metadata, String message) {
  }
}
