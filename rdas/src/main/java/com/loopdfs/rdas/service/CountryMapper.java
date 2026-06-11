package com.loopdfs.rdas.service;

import com.loopdfs.rdas.cache.CacheMetadata;
import com.loopdfs.rdas.model.dto.CacheMetadataDto;
import com.loopdfs.rdas.model.dto.ContinentDto;
import com.loopdfs.rdas.model.dto.CountryDto;
import com.loopdfs.rdas.model.dto.CurrencyDto;
import com.loopdfs.rdas.model.dto.LanguageDto;
import com.loopdfs.rdas.model.internal.ContinentRecord;
import com.loopdfs.rdas.model.internal.CountryRecord;
import com.loopdfs.rdas.model.internal.CurrencyRecord;
import com.loopdfs.rdas.model.internal.LanguageRecord;

import org.springframework.stereotype.Component;

@Component
public class CountryMapper {

  public CountryDto toDto(CountryRecord country) {
    return new CountryDto(country.isoCode(), country.name(), country.capital(), country.flagUrl(), country.phoneCode(),
        toDto(country.continent()), toDto(country.currency()), country.languages().stream().map(this::toDto).toList());
  }

  public ContinentDto toDto(ContinentRecord continent) {
    return new ContinentDto(continent.code(), continent.name());
  }

  public CurrencyDto toDto(CurrencyRecord currency) {
    return new CurrencyDto(currency.code(), currency.name());
  }

  public LanguageDto toDto(LanguageRecord language) {
    return new LanguageDto(language.code(), language.name());
  }

  public CacheMetadataDto toDto(String status, CacheMetadata metadata) {
    return new CacheMetadataDto(status, metadata.freshness().headerValue(), metadata.refreshedAt(),
        metadata.expiresAt(),
        metadata.ageSeconds(), metadata.countryCount());
  }
}
