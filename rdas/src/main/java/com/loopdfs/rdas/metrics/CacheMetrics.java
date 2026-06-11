package com.loopdfs.rdas.metrics;

import com.loopdfs.rdas.cache.CacheStore;

import io.micrometer.core.instrument.Gauge;
import io.micrometer.core.instrument.MeterRegistry;
import org.springframework.stereotype.Component;

@Component
public class CacheMetrics {

	public CacheMetrics(CacheStore cacheStore, MeterRegistry meterRegistry) {
		Gauge.builder("rdas.cache.age", cacheStore,
				store -> store.metadata().ageSeconds() < 0 ? Double.NaN : store.metadata().ageSeconds())
			.description("Age of the current country reference cache in seconds")
			.register(meterRegistry);
	}
}
