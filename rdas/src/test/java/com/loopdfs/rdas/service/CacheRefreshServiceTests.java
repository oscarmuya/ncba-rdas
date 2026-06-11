package com.loopdfs.rdas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.time.Instant;
import java.util.Optional;

import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.client.SoapCountryClient;
import com.loopdfs.rdas.client.SoapCountryClient.CountryReferenceData;

import org.junit.jupiter.api.Test;
import org.springframework.scheduling.TaskScheduler;

class CacheRefreshServiceTests {

  @Test
  void successfulRefreshSwapsCacheAndWritesSnapshot() {
    SoapCountryClient client = mock(SoapCountryClient.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    TaskScheduler scheduler = mock(TaskScheduler.class);
    CacheStore cacheStore = new CacheStore(TestFixtures.properties(), TestFixtures.CLOCK);
    when(client.fetchReferenceData()).thenReturn(referenceData());

    CacheRefreshService service = new CacheRefreshService(client, cacheStore, snapshotService,
        TestFixtures.properties(),
        TestFixtures.CLOCK, scheduler);

    CacheRefreshService.RefreshResult result = service.refreshNow();

    assertThat(result.success()).isTrue();
    assertThat(cacheStore.requireDataset().countries()).hasSize(2);
    verify(snapshotService).write(any());
  }

  @Test
  void failedRefreshKeepsStaleCache() {
    SoapCountryClient client = mock(SoapCountryClient.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    TaskScheduler scheduler = mock(TaskScheduler.class);
    CacheStore cacheStore = new CacheStore(TestFixtures.properties(), TestFixtures.CLOCK);
    cacheStore.replace(TestFixtures.dataset());
    when(client.fetchReferenceData()).thenThrow(new IllegalStateException("SOAP down"));

    CacheRefreshService service = new CacheRefreshService(client, cacheStore, snapshotService,
        TestFixtures.properties(),
        TestFixtures.CLOCK, scheduler);

    CacheRefreshService.RefreshResult result = service.refreshNow();

    assertThat(result.success()).isFalse();
    assertThat(cacheStore.requireDataset().countries()).hasSize(2);
    verify(scheduler).schedule(any(Runnable.class), any(Instant.class));
  }

  @Test
  void coldFailureLoadsSnapshot() {
    SoapCountryClient client = mock(SoapCountryClient.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    TaskScheduler scheduler = mock(TaskScheduler.class);
    CacheStore cacheStore = new CacheStore(TestFixtures.properties(), TestFixtures.CLOCK);
    when(client.fetchReferenceData()).thenThrow(new IllegalStateException("SOAP down"));
    doReturn(Optional.of(TestFixtures.dataset())).when(snapshotService).read();

    CacheRefreshService service = new CacheRefreshService(client, cacheStore, snapshotService,
        TestFixtures.properties(),
        TestFixtures.CLOCK, scheduler);

    CacheRefreshService.RefreshResult result = service.refreshNow();

    assertThat(result.success()).isFalse();
    assertThat(cacheStore.requireDataset().countries()).hasSize(2);
  }

  @Test
  void coldFailureWithoutSnapshotLeavesCacheUnavailable() {
    SoapCountryClient client = mock(SoapCountryClient.class);
    SnapshotService snapshotService = mock(SnapshotService.class);
    TaskScheduler scheduler = mock(TaskScheduler.class);
    CacheStore cacheStore = new CacheStore(TestFixtures.properties(), TestFixtures.CLOCK);
    when(client.fetchReferenceData()).thenThrow(new IllegalStateException("SOAP down"));
    doReturn(Optional.empty()).when(snapshotService).read();

    CacheRefreshService service = new CacheRefreshService(client, cacheStore, snapshotService,
        TestFixtures.properties(),
        TestFixtures.CLOCK, scheduler);

    CacheRefreshService.RefreshResult result = service.refreshNow();

    assertThat(result.success()).isFalse();
    assertThat(cacheStore.currentDataset()).isEmpty();
  }

  private CountryReferenceData referenceData() {
    return new CountryReferenceData(TestFixtures.dataset().countries(), TestFixtures.dataset().continents(),
        TestFixtures.dataset().currencies(), TestFixtures.dataset().languages());
  }
}
