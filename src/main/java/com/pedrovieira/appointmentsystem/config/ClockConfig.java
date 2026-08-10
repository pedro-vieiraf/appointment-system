package com.pedrovieira.appointmentsystem.config;

import java.time.Clock;
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
}