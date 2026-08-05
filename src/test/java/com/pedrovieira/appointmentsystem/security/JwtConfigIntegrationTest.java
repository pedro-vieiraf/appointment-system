package com.pedrovieira.appointmentsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pedrovieira.appointmentsystem.config.JwtProperties;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@ExtendWith(SpringExtension.class)
@ContextConfiguration(classes = {JwtConfig.class, JwtProperties.class})
@TestPropertySource(properties = {
    "jwt.secret=superstrongjwtsecretomgidontknowwhattowrite",
    "jwt.expiration=15m"
})
class JwtConfigIntegrationTest {

  @Autowired
  private JwtService jwtService;

  @Test
  void shouldCreateJwtServiceBeanWithConfiguredProperties() {
    // Assert
    assertNotNull(jwtService);
  }


  @Test
  void shouldConfigureExpirationTimeCorrectly() {
    // Assert
    assertEquals(
        900L,
        jwtService.getExpirationInSeconds()
    );
  }
}