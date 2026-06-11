package com.loopdfs.rdas.model.internal;

import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.function.Function;
import java.util.stream.Collectors;

public record CountryDataset(List<CountryRecord> countries, List<ContinentRecord> continents,
    List<CurrencyRecord> currencies, List<LanguageRecord> languages, Instant refreshedAt, Instant expiresAt) {

  public CountryDataset {
    countries = sortedCopy(countries, Comparator.comparing(CountryRecord::name, String.CASE_INSENSITIVE_ORDER));
    continents = sortedCopy(continents, Comparator.comparing(ContinentRecord::name, String.CASE_INSENSITIVE_ORDER));
    currencies = sortedCopy(currencies, Comparator.comparing(CurrencyRecord::name, String.CASE_INSENSITIVE_ORDER));
    languages = sortedCopy(languages, Comparator.comparing(LanguageRecord::name, String.CASE_INSENSITIVE_ORDER));
  }

  public static CountryDataset create(List<CountryRecord> countries, List<ContinentRecord> continents,
      List<CurrencyRecord> currencies, List<LanguageRecord> languages, Duration ttl, Clock clock) {
    Instant now = Instant.now(clock);
    return new CountryDataset(countries, continents, currencies, languages, now, now.plus(ttl));
  }

  public Optional<CountryRecord> countryByIsoCode(String isoCode) {
    return countriesByIsoCode().values().stream()
        .filter(country -> country.isoCode().equalsIgnoreCase(normalizeCode(isoCode)))
        .findFirst();
  }

  public boolean isExpired(Clock clock) {
    return !Instant.now(clock).isBefore(expiresAt);
  }

  public long ageSeconds(Clock clock) {
    return Math.max(0, Duration.between(refreshedAt, Instant.now(clock)).toSeconds());
  }

  private Map<String, CountryRecord> countriesByIsoCode() {
    return countries.stream()
        .collect(
            Collectors.toMap(country -> normalizeCode(country.isoCode()), Function.identity(), (left, right) -> left,
                LinkedHashMap::new));
  }

  private static <T> List<T> sortedCopy(List<T> values, Comparator<T> comparator) {
    return values == null ? List.of() : values.stream().sorted(comparator).toList();
  }

  private static String normalizeCode(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }
}
