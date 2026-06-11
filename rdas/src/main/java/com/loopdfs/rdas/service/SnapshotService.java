package com.loopdfs.rdas.service;

import java.nio.file.Files;
import java.nio.file.Path;
import java.util.Optional;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.loopdfs.rdas.config.RdasProperties;
import com.loopdfs.rdas.model.internal.CountryDataset;

import org.springframework.stereotype.Service;

@Service
public class SnapshotService {

  private final ObjectMapper objectMapper;
  private final Path snapshotPath;

  public SnapshotService(ObjectMapper objectMapper, RdasProperties properties) {
    this.objectMapper = objectMapper;
    this.snapshotPath = properties.cache().snapshotPath();
  }

  public void write(CountryDataset dataset) {
    try {
      Path absolutePath = snapshotPath.toAbsolutePath();
      Path parent = absolutePath.getParent();
      if (parent != null) {
        Files.createDirectories(parent);
      }
      objectMapper.writerWithDefaultPrettyPrinter().writeValue(absolutePath.toFile(), dataset);
    } catch (Exception ex) {
      throw new IllegalStateException("Unable to write country snapshot", ex);
    }
  }

  public Optional<CountryDataset> read() {
    try {
      Path absolutePath = snapshotPath.toAbsolutePath();
      if (!Files.isRegularFile(absolutePath)) {
        return Optional.empty();
      }
      return Optional.of(objectMapper.readValue(absolutePath.toFile(), CountryDataset.class));
    } catch (Exception ex) {
      return Optional.empty();
    }
  }
}
