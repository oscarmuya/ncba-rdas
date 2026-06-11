package com.loopdfs.rdas.config;

import java.nio.file.Path;
import java.time.Duration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "rdas")
public record RdasProperties(Soap soap, Cache cache, Refresh refresh) {

  public RdasProperties {
    soap = soap == null ? new Soap(null, null, null, null) : soap;
    cache = cache == null ? new Cache(null, null, null) : cache;
    refresh = refresh == null ? new Refresh(null, null, null) : refresh;
  }

  public record Soap(String endpoint, String namespaceUri, Duration connectTimeout, Duration readTimeout) {

    public Soap {
      endpoint = endpoint == null ? "https://webservices.oorsprong.org/websamples.countryinfo/CountryInfoService.wso"
          : endpoint;
      namespaceUri = namespaceUri == null ? "http://www.oorsprong.org/websamples.countryinfo" : namespaceUri;
      connectTimeout = connectTimeout == null ? Duration.ofSeconds(5) : connectTimeout;
      readTimeout = readTimeout == null ? Duration.ofSeconds(20) : readTimeout;
    }
  }

  public record Cache(Duration ttl, Path snapshotPath, Duration staleThreshold) {

    public Cache {
      ttl = ttl == null ? Duration.ofHours(24) : ttl;
      snapshotPath = snapshotPath == null ? Path.of("./data/countries-snapshot.json") : snapshotPath;
      staleThreshold = staleThreshold == null ? Duration.ofHours(26) : staleThreshold;
    }
  }

  public record Refresh(Boolean startupEnabled, Boolean scheduledEnabled, Duration fixedDelay) {

    public Refresh {
      startupEnabled = startupEnabled == null ? Boolean.TRUE : startupEnabled;
      scheduledEnabled = scheduledEnabled == null ? Boolean.TRUE : scheduledEnabled;
      fixedDelay = fixedDelay == null ? Duration.ofHours(24) : fixedDelay;
    }
  }
}
