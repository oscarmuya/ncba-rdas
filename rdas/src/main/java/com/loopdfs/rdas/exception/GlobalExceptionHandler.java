package com.loopdfs.rdas.exception;

import java.time.Clock;
import java.time.Instant;

import com.loopdfs.rdas.model.dto.ErrorResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.ConstraintViolationException;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.method.annotation.HandlerMethodValidationException;

@RestControllerAdvice
public class GlobalExceptionHandler {

  private final Clock clock;

  public GlobalExceptionHandler(Clock clock) {
    this.clock = clock;
  }

  @ExceptionHandler(CountryNotFoundException.class)
  public ResponseEntity<ErrorResponse> notFound(CountryNotFoundException ex, HttpServletRequest request) {
    return error(HttpStatus.NOT_FOUND, ex.getMessage(), request);
  }

  @ExceptionHandler({ InvalidSortException.class, ConstraintViolationException.class,
      MethodArgumentNotValidException.class,
      HandlerMethodValidationException.class })
  public ResponseEntity<ErrorResponse> badRequest(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.BAD_REQUEST, ex.getMessage(), request);
  }

  @ExceptionHandler({ ReferenceDataUnavailableException.class, SoapCountryClientException.class })
  public ResponseEntity<ErrorResponse> unavailable(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.SERVICE_UNAVAILABLE, "Reference data temporarily unavailable. Please retry shortly.",
        request);
  }

  @ExceptionHandler(Exception.class)
  public ResponseEntity<ErrorResponse> generic(Exception ex, HttpServletRequest request) {
    return error(HttpStatus.INTERNAL_SERVER_ERROR, "Unexpected service error", request);
  }

  private ResponseEntity<ErrorResponse> error(HttpStatus status, String message, HttpServletRequest request) {
    return ResponseEntity.status(status)
        .body(new ErrorResponse(Instant.now(clock), status.value(), status.getReasonPhrase(), message,
            request.getRequestURI()));
  }
}
