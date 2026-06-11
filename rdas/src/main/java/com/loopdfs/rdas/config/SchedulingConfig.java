package com.loopdfs.rdas.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.scheduling.TaskScheduler;
import org.springframework.scheduling.concurrent.ThreadPoolTaskScheduler;

@Configuration
class SchedulingConfig {

  @Bean
  TaskScheduler taskScheduler() {
    ThreadPoolTaskScheduler scheduler = new ThreadPoolTaskScheduler();
    scheduler.setPoolSize(2);
    scheduler.setThreadNamePrefix("rdas-refresh-");
    scheduler.initialize();
    return scheduler;
  }

  @Bean
  Long refreshFixedDelayMillis(RdasProperties properties) {
    return properties.refresh().fixedDelay().toMillis();
  }
}
