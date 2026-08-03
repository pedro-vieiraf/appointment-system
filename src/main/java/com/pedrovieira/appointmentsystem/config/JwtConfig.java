package com.pedrovieira.appointmentsystem.config;

import com.pedrovieira.appointmentsystem.service.JwtService;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties(JwtProperties.class)
public class JwtConfig {

  @Bean
  public JwtService jwtService(JwtProperties jwtProperties) {
    return new JwtService(jwtProperties.getSecret(), jwtProperties.getExpiration());
  }
}