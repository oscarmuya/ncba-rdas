package com.loopdfs.rdas.controller;

import java.util.List;

import com.loopdfs.rdas.model.dto.ContinentDto;
import com.loopdfs.rdas.model.dto.CurrencyDto;
import com.loopdfs.rdas.model.dto.LanguageDto;
import com.loopdfs.rdas.service.CountryService;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1")
public class ReferenceController {

  private final CountryService countryService;

  public ReferenceController(CountryService countryService) {
    this.countryService = countryService;
  }

  @GetMapping("/continents")
  public List<ContinentDto> continents() {
    return countryService.continents();
  }

  @GetMapping("/currencies")
  public List<CurrencyDto> currencies() {
    return countryService.currencies();
  }

  @GetMapping("/languages")
  public List<LanguageDto> languages() {
    return countryService.languages();
  }
}
