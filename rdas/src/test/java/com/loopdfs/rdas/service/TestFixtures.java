package com.loopdfs.rdas.service;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import java.util.List;

import com.loopdfs.rdas.config.RdasProperties;
import com.loopdfs.rdas.model.internal.ContinentRecord;
import com.loopdfs.rdas.model.internal.CountryDataset;
import com.loopdfs.rdas.model.internal.CountryRecord;
import com.loopdfs.rdas.model.internal.CurrencyRecord;
import com.loopdfs.rdas.model.internal.LanguageRecord;

final class TestFixtures {

  static final Clock CLOCK = Clock.fixed(Instant.parse("2026-06-11T12:00:00Z"), ZoneOffset.UTC);
  static final ContinentRecord AFRICA = new ContinentRecord("AF", "Africa");
  static final ContinentRecord EUROPE = new ContinentRecord("EU", "Europe");
  static final CurrencyRecord KES = new CurrencyRecord("KES", "Kenyan Shilling");
  static final CurrencyRecord EUR = new CurrencyRecord("EUR", "Euro");
  static final LanguageRecord EN = new LanguageRecord("EN", "English");
  static final LanguageRecord SW = new LanguageRecord("SW", "Swahili");
  static final LanguageRecord FR = new LanguageRecord("FR", "French");

  private TestFixtures() {
  }

  static RdasProperties properties() {
    return new RdasProperties(
        new RdasProperties.Soap("https://example.test/soap", "http://example.test", Duration.ofSeconds(1),
            Duration.ofSeconds(1)),
        new RdasProperties.Cache(Duration.ofHours(24), java.nio.file.Path.of("target/test-snapshot.json"),
            Duration.ofHours(26)),
        new RdasProperties.Refresh(false, false, Duration.ofHours(24)));
  }

  static CountryDataset dataset() {
    return CountryDataset.create(List.of(
        new CountryRecord("KE", "Kenya", "Nairobi", "https://flags.test/ke.gif", "254", AFRICA, KES,
            List.of(EN, SW)),
        new CountryRecord("FR", "France", "Paris", "https://flags.test/fr.gif", "33", EUROPE, EUR,
            List.of(FR))),
        List.of(AFRICA, EUROPE), List.of(KES, EUR), List.of(EN, SW, FR), Duration.ofHours(24), CLOCK);
  }
}
