package com.pedrovieira.appointmentsystem.config;

import java.time.Duration;
import lombok.Getter;
import lombok.Setter;
import org.springframework.boot.context.properties.ConfigurationProperties;

/**
 * Getter and setter for the JwtProperties.
 */
@Getter
@Setter
@ConfigurationProperties(prefix = "jwt")
public class JwtProperties {

  private String secret;
  private Duration expiration;
}
