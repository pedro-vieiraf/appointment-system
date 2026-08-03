package com.pedrovieira.appointmentsystem.config;

import com.pedrovieira.appointmentsystem.service.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

/**
 * Configuration for jwtService to become a Bean.
 */
@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  /**
   * Bean for JWTService.
   */
  @Bean
  public JwtService jwtService(JwtProperties jwtProperties) {
    return new JwtService(jwtProperties.getSecret(), jwtProperties.getExpiration());
  }
}