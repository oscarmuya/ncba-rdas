package com.loopdfs.rdas;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = { "rdas.refresh.startup-enabled=false", "rdas.refresh.scheduled-enabled=false",
    "spring.docker.compose.enabled=false" })
class RdasApplicationTests {

  @Test
  void contextLoads() {
  }

}
