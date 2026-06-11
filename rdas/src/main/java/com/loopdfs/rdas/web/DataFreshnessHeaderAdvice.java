package com.loopdfs.rdas.web;

import com.loopdfs.rdas.cache.CacheStore;

import jakarta.servlet.http.HttpServletRequest;
import org.springframework.core.MethodParameter;
import org.springframework.http.MediaType;
import org.springframework.http.converter.HttpMessageConverter;
import org.springframework.http.server.ServerHttpRequest;
import org.springframework.http.server.ServerHttpResponse;
import org.springframework.http.server.ServletServerHttpRequest;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.servlet.mvc.method.annotation.ResponseBodyAdvice;

@ControllerAdvice
public class DataFreshnessHeaderAdvice implements ResponseBodyAdvice<Object> {

  public static final String HEADER_NAME = "X-Data-Freshness";

  private final CacheStore cacheStore;

  public DataFreshnessHeaderAdvice(CacheStore cacheStore) {
    this.cacheStore = cacheStore;
  }

  @Override
  public boolean supports(MethodParameter returnType, Class<? extends HttpMessageConverter<?>> converterType) {
    return true;
  }

  @Override
  public Object beforeBodyWrite(Object body, MethodParameter returnType, MediaType selectedContentType,
      Class<? extends HttpMessageConverter<?>> selectedConverterType, ServerHttpRequest request,
      ServerHttpResponse response) {
    if (request instanceof ServletServerHttpRequest servletRequest
        && isApiRequest(servletRequest.getServletRequest())) {
      response.getHeaders().set(HEADER_NAME, cacheStore.freshness().headerValue());
    }
    return body;
  }

  private boolean isApiRequest(HttpServletRequest request) {
    return request.getRequestURI().startsWith("/api/v1");
  }
}
