package com.loopdfs.rdas.controller;

import com.loopdfs.rdas.model.dto.CacheMetadataDto;
import com.loopdfs.rdas.service.CacheRefreshService;
import com.loopdfs.rdas.service.CountryMapper;

import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/cache")
public class AdminCacheController {

  private final CacheRefreshService cacheRefreshService;
  private final CountryMapper mapper;

  public AdminCacheController(CacheRefreshService cacheRefreshService, CountryMapper mapper) {
    this.cacheRefreshService = cacheRefreshService;
    this.mapper = mapper;
  }

  @PostMapping("/refresh")
  public CacheMetadataDto refresh() {
    CacheRefreshService.RefreshResult result = cacheRefreshService.refreshNow();
    return mapper.toDto(result.success() ? "refreshed" : "refresh_failed", result.metadata());
  }
}
