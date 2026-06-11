package com.loopdfs.rdas.service;

import java.util.Comparator;
import java.util.List;
import java.util.Locale;
import java.util.function.Predicate;

import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.exception.CountryNotFoundException;
import com.loopdfs.rdas.exception.InvalidSortException;
import com.loopdfs.rdas.model.dto.ContinentDto;
import com.loopdfs.rdas.model.dto.CountryDto;
import com.loopdfs.rdas.model.dto.CurrencyDto;
import com.loopdfs.rdas.model.dto.LanguageDto;
import com.loopdfs.rdas.model.dto.PagedResponse;
import com.loopdfs.rdas.model.internal.CountryDataset;
import com.loopdfs.rdas.model.internal.CountryRecord;

import org.springframework.stereotype.Service;

@Service
public class CountryService {

  private final CacheStore cacheStore;
  private final CountryMapper mapper;

  public CountryService(CacheStore cacheStore, CountryMapper mapper) {
    this.cacheStore = cacheStore;
    this.mapper = mapper;
  }

  public PagedResponse<CountryDto> findCountries(String search, String continent, String currency, String language,
      int page,
      int size, String sort) {
    CountryDataset dataset = cacheStore.requireDataset();
    List<CountryDto> filtered = dataset.countries()
        .stream()
        .filter(matchesSearch(search))
        .filter(matchesContinent(continent))
        .filter(matchesCurrency(currency))
        .filter(matchesLanguage(language))
        .sorted(countryComparator(sort))
        .map(mapper::toDto)
        .toList();
    return page(filtered, page, size);
  }

  public CountryDto getCountry(String isoCode) {
    return cacheStore.requireDataset()
        .countryByIsoCode(isoCode)
        .map(mapper::toDto)
        .orElseThrow(() -> new CountryNotFoundException(isoCode));
  }

  public PagedResponse<CountryDto> countriesByCurrency(String currencyCode, int page, int size, String sort) {
    return findCountries(null, null, currencyCode, null, page, size, sort);
  }

  public List<ContinentDto> continents() {
    return cacheStore.requireDataset().continents().stream().map(mapper::toDto).toList();
  }

  public List<CurrencyDto> currencies() {
    return cacheStore.requireDataset().currencies().stream().map(mapper::toDto).toList();
  }

  public List<LanguageDto> languages() {
    return cacheStore.requireDataset().languages().stream().map(mapper::toDto).toList();
  }

  private Predicate<CountryRecord> matchesSearch(String search) {
    String normalized = normalizeText(search);
    if (normalized.isBlank()) {
      return country -> true;
    }
    return country -> normalizeText(country.name()).contains(normalized);
  }

  private Predicate<CountryRecord> matchesContinent(String continent) {
    String normalized = normalizeCode(continent);
    if (normalized.isBlank()) {
      return country -> true;
    }
    return country -> country.continent() != null && normalizeCode(country.continent().code()).equals(normalized);
  }

  private Predicate<CountryRecord> matchesCurrency(String currency) {
    String normalized = normalizeCode(currency);
    if (normalized.isBlank()) {
      return country -> true;
    }
    return country -> country.currency() != null && normalizeCode(country.currency().code()).equals(normalized);
  }

  private Predicate<CountryRecord> matchesLanguage(String language) {
    String normalized = normalizeCode(language);
    if (normalized.isBlank()) {
      return country -> true;
    }
    return country -> country.languages().stream().anyMatch(item -> normalizeCode(item.code()).equals(normalized));
  }

  private Comparator<CountryRecord> countryComparator(String sort) {
    String[] parts = (sort == null || sort.isBlank() ? "name,asc" : sort).split(",", -1);
    String field = parts[0].trim();
    boolean descending = parts.length > 1 && "desc".equalsIgnoreCase(parts[1].trim());
    if (parts.length > 1 && !parts[1].isBlank() && !"asc".equalsIgnoreCase(parts[1].trim())
        && !"desc".equalsIgnoreCase(parts[1].trim())) {
      throw new InvalidSortException(sort);
    }

    Comparator<CountryRecord> comparator = switch (field) {
      case "name" -> Comparator.comparing(CountryRecord::name, String.CASE_INSENSITIVE_ORDER);
      case "isoCode" -> Comparator.comparing(CountryRecord::isoCode, String.CASE_INSENSITIVE_ORDER);
      case "capital" -> Comparator.comparing(CountryRecord::capital, String.CASE_INSENSITIVE_ORDER);
      case "continent" -> Comparator.comparing(country -> country.continent().name(), String.CASE_INSENSITIVE_ORDER);
      case "currency" -> Comparator.comparing(country -> country.currency().code(), String.CASE_INSENSITIVE_ORDER);
      default -> throw new InvalidSortException(sort);
    };
    return descending ? comparator.reversed() : comparator;
  }

  private PagedResponse<CountryDto> page(List<CountryDto> values, int page, int size) {
    int fromIndex = Math.min(page * size, values.size());
    int toIndex = Math.min(fromIndex + size, values.size());
    int totalPages = values.isEmpty() ? 0 : (int) Math.ceil((double) values.size() / size);
    return new PagedResponse<>(values.subList(fromIndex, toIndex), page, size, values.size(), totalPages);
  }

  private String normalizeCode(String value) {
    return value == null ? "" : value.trim().toUpperCase(Locale.ROOT);
  }

  private String normalizeText(String value) {
    return value == null ? "" : value.trim().toLowerCase(Locale.ROOT);
  }
}
