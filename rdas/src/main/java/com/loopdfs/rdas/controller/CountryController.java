package com.loopdfs.rdas.controller;

import com.loopdfs.rdas.model.dto.CountryDto;
import com.loopdfs.rdas.model.dto.PagedResponse;
import com.loopdfs.rdas.service.CountryService;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.Pattern;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Validated
@RestController
@RequestMapping("/api/v1")
public class CountryController {

  private final CountryService countryService;

  public CountryController(CountryService countryService) {
    this.countryService = countryService;
  }

  @GetMapping("/countries")
  public PagedResponse<CountryDto> countries(@RequestParam(required = false) String search,
      @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{2}$") String continent,
      @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{3}$") String currency,
      @RequestParam(required = false) @Pattern(regexp = "^[A-Za-z]{2,3}$") String language,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(250) int size,
      @RequestParam(defaultValue = "name,asc") String sort) {
    return countryService.findCountries(search, continent, currency, language, page, size, sort);
  }

  @GetMapping("/countries/{isoCode}")
  public CountryDto country(@PathVariable @Pattern(regexp = "^[A-Za-z]{2}$") String isoCode) {
    return countryService.getCountry(isoCode);
  }

  @GetMapping("/currencies/{currencyCode}/countries")
  public PagedResponse<CountryDto> countriesByCurrency(
      @PathVariable @Pattern(regexp = "^[A-Za-z]{3}$") String currencyCode,
      @RequestParam(defaultValue = "0") @Min(0) int page,
      @RequestParam(defaultValue = "20") @Min(1) @Max(250) int size,
      @RequestParam(defaultValue = "name,asc") String sort) {
    return countryService.countriesByCurrency(currencyCode, page, size, sort);
  }
}
