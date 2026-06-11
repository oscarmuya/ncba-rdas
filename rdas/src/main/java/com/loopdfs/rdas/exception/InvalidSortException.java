package com.loopdfs.rdas.exception;

public class InvalidSortException extends RuntimeException {

  public InvalidSortException(String sort) {
    super("Unsupported sort value: " + sort);
  }
}
