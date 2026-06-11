package com.loopdfs.rdas.model.internal;

import java.util.List;

public record CountryRecord(String isoCode, String name, String capital, String flagUrl, String phoneCode,
		ContinentRecord continent, CurrencyRecord currency, List<LanguageRecord> languages) {

	public CountryRecord {
		languages = languages == null ? List.of() : List.copyOf(languages);
	}
}
