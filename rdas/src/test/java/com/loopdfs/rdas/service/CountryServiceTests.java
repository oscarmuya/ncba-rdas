package com.loopdfs.rdas.service;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.loopdfs.rdas.cache.CacheStore;
import com.loopdfs.rdas.exception.CountryNotFoundException;
import com.loopdfs.rdas.exception.InvalidSortException;
import com.loopdfs.rdas.model.dto.PagedResponse;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

class CountryServiceTests {

  private CountryService service;

  @BeforeEach
  void setUp() {
    CacheStore cacheStore = new CacheStore(TestFixtures.properties(), TestFixtures.CLOCK);
    cacheStore.replace(TestFixtures.dataset());
    service = new CountryService(cacheStore, new CountryMapper());
  }

  @Test
  void filtersSearchContinentCurrencyAndLanguage() {
    PagedResponse<?> response = service.findCountries("ken", "af", "kes", "sw", 0, 20, "name,asc");

    assertThat(response.totalElements()).isEqualTo(1);
    assertThat(response.content()).extracting("isoCode").containsExactly("KE");
  }

  @Test
  void sortsAndPaginates() {
    PagedResponse<?> response = service.findCountries(null, null, null, null, 0, 1, "name,desc");

    assertThat(response.totalElements()).isEqualTo(2);
    assertThat(response.totalPages()).isEqualTo(2);
    assertThat(response.content()).extracting("name").containsExactly("Kenya");
  }

  @Test
  void findsCountryByIsoCodeCaseInsensitively() {
    assertThat(service.getCountry("ke").capital()).isEqualTo("Nairobi");
  }

  @Test
  void throwsWhenCountryIsMissing() {
    assertThatThrownBy(() -> service.getCountry("ZZ")).isInstanceOf(CountryNotFoundException.class);
  }

  @Test
  void rejectsInvalidSortField() {
    assertThatThrownBy(() -> service.findCountries(null, null, null, null, 0, 20, "population,asc"))
        .isInstanceOf(InvalidSortException.class);
  }
}
