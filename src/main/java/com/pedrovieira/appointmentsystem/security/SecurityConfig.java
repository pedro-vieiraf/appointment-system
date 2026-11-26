package com.pedrovieira.appointmentsystem.security;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.http.HttpMethod;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.config.annotation.web.configurers.AbstractHttpConfigurer;
import org.springframework.security.config.http.SessionCreationPolicy;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.security.web.authentication.UsernamePasswordAuthenticationFilter;

/**
 * Security configuration.
 */
@Configuration
public class SecurityConfig {

  /**
   * Security Filter Chain for all basic security configurations.
   */
  @Bean
  public SecurityFilterChain securityFilterChain(HttpSecurity http,
      JwtAuthenticationFilter jwtAuthenticationFilter,
      @Value("${springdoc.api-docs.enabled:false}") boolean apiDocsEnabled) throws Exception {
    http
        .csrf(AbstractHttpConfigurer::disable)
        .formLogin(AbstractHttpConfigurer::disable)
        .httpBasic(AbstractHttpConfigurer::disable)
        .logout(AbstractHttpConfigurer::disable)
        .sessionManagement(session ->
            session.sessionCreationPolicy(SessionCreationPolicy.STATELESS)
        )
        .authorizeHttpRequests(auth -> {
          auth.requestMatchers("/auth/**").permitAll();
          auth.requestMatchers(HttpMethod.GET, "/public/availability/*/slots",
              "/public/availability/*/requests/verify").permitAll();
          auth.requestMatchers(HttpMethod.POST, "/public/availability/*/requests",
              "/public/availability/*/requests/verify").permitAll();
          if (apiDocsEnabled) {
            auth.requestMatchers(HttpMethod.GET,
                "/v3/api-docs", "/v3/api-docs.yaml", "/v3/api-docs/swagger-config",
                "/swagger-ui.html", "/swagger-ui/**").permitAll();
          }
          auth.anyRequest().authenticated();
        })
        .addFilterBefore(
            jwtAuthenticationFilter,
            UsernamePasswordAuthenticationFilter.class
        );

    return http.build();
  }

  /**
   * Password encoder.
   */
  @Bean
  public PasswordEncoder passwordEncoder() {
    return new BCryptPasswordEncoder();
  }
}
