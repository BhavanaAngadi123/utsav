package com.utsav;

import com.utsav.config.UtsavProperties;
import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.scheduling.annotation.EnableAsync;

/**
 * Utsav — event-vendor marketplace.
 *
 * <p>Single deployable: Spring Boot REST API + Vaadin UI, PostgreSQL, Flyway,
 * JWT auth, Stripe payments (test mode), Spring AI concierge (Ollama).
 */
@SpringBootApplication
@EnableConfigurationProperties(UtsavProperties.class)
@EnableAsync
public class UtsavApplication {

  public static void main(String[] args) {
    SpringApplication.run(UtsavApplication.class, args);
  }
}
