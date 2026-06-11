package com.loopdfs.rdas.exception;

public class CountryNotFoundException extends RuntimeException {

  public CountryNotFoundException(String isoCode) {
    super("Country not found: " + isoCode);
  }
}
