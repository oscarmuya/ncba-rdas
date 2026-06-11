package com.loopdfs.rdas.client;

import java.util.List;

import com.loopdfs.rdas.model.internal.ContinentRecord;
import com.loopdfs.rdas.model.internal.CountryRecord;
import com.loopdfs.rdas.model.internal.CurrencyRecord;
import com.loopdfs.rdas.model.internal.LanguageRecord;

public interface SoapCountryClient {

  CountryReferenceData fetchReferenceData();

  record CountryReferenceData(List<CountryRecord> countries, List<ContinentRecord> continents,
      List<CurrencyRecord> currencies, List<LanguageRecord> languages) {
  }
}
