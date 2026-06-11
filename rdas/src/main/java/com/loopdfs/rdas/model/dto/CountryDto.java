package com.loopdfs.rdas.model.dto;

import java.util.List;

public record CountryDto(String isoCode, String name, String capital, String flagUrl, String phoneCode,
		ContinentDto continent, CurrencyDto currency, List<LanguageDto> languages) {
}
