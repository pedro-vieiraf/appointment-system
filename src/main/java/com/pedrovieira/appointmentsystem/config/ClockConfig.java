package com.pedrovieira.appointmentsystem.config;

import java.time.Clock;
import org.springframework.boot.validation.autoconfigure.ValidationConfigurationCustomizer;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Clock configurations.
 */
@Configuration
public class ClockConfig {

  /**
   * Bean for clock.
   */
  @Bean
  public Clock clock() {
    return Clock.systemUTC();
  }

  /**
   * Uses the application clock for date constraints such as Future.
   */
  @Bean
  public ValidationConfigurationCustomizer validationClockProvider(Clock clock) {
    return configuration -> configuration.clockProvider(() -> clock);
  }
}
