package com.loopdfs.rdas.config;

import java.time.Duration;

import org.springframework.boot.http.client.HttpClientSettings;
import org.springframework.boot.webservices.client.WebServiceMessageSenderFactory;
import org.springframework.boot.webservices.client.WebServiceTemplateBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.ws.client.core.WebServiceTemplate;

@Configuration
class SoapConfig {

  @Bean
  WebServiceTemplate countryInfoWebServiceTemplate(WebServiceTemplateBuilder builder, RdasProperties properties) {
    Duration connectTimeout = properties.soap().connectTimeout();
    Duration readTimeout = properties.soap().readTimeout();

    HttpClientSettings clientSettings = HttpClientSettings.defaults()
        .withTimeouts(connectTimeout, readTimeout);

    return builder
        .httpMessageSenderFactory(WebServiceMessageSenderFactory.http(clientSettings))
        .build();
  }
}
