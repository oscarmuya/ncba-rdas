package com.loopdfs.rdas.controller;

import static org.hamcrest.Matchers.containsString;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import java.time.Instant;
import java.util.List;

import com.loopdfs.rdas.cache.CacheMetadata;
import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.cache.DataFreshness;
import com.loopdfs.rdas.exception.CountryNotFoundException;
import com.loopdfs.rdas.exception.GlobalExceptionHandler;
import com.loopdfs.rdas.exception.InvalidSortException;
import com.loopdfs.rdas.exception.ReferenceDataUnavailableException;
import com.loopdfs.rdas.model.dto.CacheMetadataDto;
import com.loopdfs.rdas.model.dto.ContinentDto;
import com.loopdfs.rdas.model.dto.CountryDto;
import com.loopdfs.rdas.model.dto.CurrencyDto;
import com.loopdfs.rdas.model.dto.LanguageDto;
import com.loopdfs.rdas.model.dto.PagedResponse;
import com.loopdfs.rdas.service.CacheRefreshService;
import com.loopdfs.rdas.service.CountryMapper;
import com.loopdfs.rdas.service.CountryService;
import com.loopdfs.rdas.web.DataFreshnessHeaderAdvice;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;

class CountryControllerTests {

  private CountryService countryService;
  private CacheRefreshService cacheRefreshService;
  private CountryMapper mapper;
  private MockMvc mockMvc;

  @BeforeEach
  void setUp() {
    countryService = org.mockito.Mockito.mock(CountryService.class);
    cacheRefreshService = org.mockito.Mockito.mock(CacheRefreshService.class);
    CacheStore cacheStore = org.mockito.Mockito.mock(CacheStore.class);
    when(cacheStore.freshness()).thenReturn(DataFreshness.FRESH);
    mapper = org.mockito.Mockito.mock(CountryMapper.class);

    mockMvc = MockMvcBuilders
        .standaloneSetup(new CountryController(countryService), new ReferenceController(countryService),
            new AdminCacheController(cacheRefreshService, mapper))
        .setControllerAdvice(new GlobalExceptionHandler(java.time.Clock.systemUTC()),
            new DataFreshnessHeaderAdvice(cacheStore))
        .build();
  }

  @Test
  void listsCountries() throws Exception {
    when(countryService.findCountries(isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt(), anyString()))
        .thenReturn(new PagedResponse<>(List.of(country()), 0, 20, 1, 1));

    mockMvc.perform(get("/api/v1/countries"))
        .andExpect(status().isOk())
        .andExpect(header().string("X-Data-Freshness", "fresh"))
        .andExpect(jsonPath("$.content[0].isoCode").value("KE"))
        .andExpect(jsonPath("$.totalElements").value(1));
  }

  @Test
  void returnsCountry() throws Exception {
    when(countryService.getCountry("KE")).thenReturn(country());

    mockMvc.perform(get("/api/v1/countries/KE"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.capital").value("Nairobi"));
  }

  @Test
  void returnsReferenceLookups() throws Exception {
    when(countryService.continents()).thenReturn(List.of(new ContinentDto("AF", "Africa")));
    when(countryService.currencies()).thenReturn(List.of(new CurrencyDto("KES", "Kenyan Shilling")));
    when(countryService.languages()).thenReturn(List.of(new LanguageDto("SW", "Swahili")));

    mockMvc.perform(get("/api/v1/continents")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("AF"));
    mockMvc.perform(get("/api/v1/currencies")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("KES"));
    mockMvc.perform(get("/api/v1/languages")).andExpect(status().isOk()).andExpect(jsonPath("$[0].code").value("SW"));
  }

  @Test
  void returnsCountriesByCurrency() throws Exception {
    when(countryService.countriesByCurrency("KES", 0, 20, "name,asc"))
        .thenReturn(new PagedResponse<>(List.of(country()), 0, 20, 1, 1));

    mockMvc.perform(get("/api/v1/currencies/KES/countries"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content[0].currency.code").value("KES"));
  }

  @Test
  void refreshesCache() throws Exception {
    CacheMetadata metadata = new CacheMetadata(DataFreshness.FRESH, Instant.parse("2026-06-11T12:00:00Z"),
        Instant.parse("2026-06-12T12:00:00Z"), 0, 2);
    when(cacheRefreshService.refreshNow())
        .thenReturn(new CacheRefreshService.RefreshResult(true, "manual", metadata, null));
    when(mapper.toDto("refreshed", metadata))
        .thenReturn(new CacheMetadataDto("refreshed", "fresh", metadata.refreshedAt(), metadata.expiresAt(), 0, 2));

    mockMvc.perform(post("/api/v1/admin/cache/refresh"))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("refreshed"))
        .andExpect(jsonPath("$.countryCount").value(2));
  }

  @Test
  void returnsBadRequestForValidationAndSortErrors() throws Exception {
    when(countryService.findCountries(isNull(), isNull(), isNull(), isNull(), anyInt(), anyInt(), anyString()))
        .thenThrow(new InvalidSortException("population,asc"));

		mockMvc.perform(get("/api/v1/countries").param("sort", "population,asc"))
			.andExpect(status().isBadRequest())
			.andExpect(jsonPath("$.message", containsString("Unsupported sort value")));
  }

  @Test
  void returnsNotFound() throws Exception {
    when(countryService.getCountry("ZZ")).thenThrow(new CountryNotFoundException("ZZ"));

    mockMvc.perform(get("/api/v1/countries/ZZ")).andExpect(status().isNotFound());
  }

  @Test
  void returnsUnavailableWhenCacheIsCold() throws Exception {
    when(countryService.getCountry("KE"))
        .thenThrow(
            new ReferenceDataUnavailableException("Reference data temporarily unavailable. Please retry shortly."));

    mockMvc.perform(get("/api/v1/countries/KE"))
        .andExpect(status().isServiceUnavailable())
        .andExpect(jsonPath("$.message").value("Reference data temporarily unavailable. Please retry shortly."));
  }

  private CountryDto country() {
    return new CountryDto("KE", "Kenya", "Nairobi", "https://flags.test/ke.gif", "254",
        new ContinentDto("AF", "Africa"),
        new CurrencyDto("KES", "Kenyan Shilling"), List.of(new LanguageDto("SW", "Swahili")));
  }
}
