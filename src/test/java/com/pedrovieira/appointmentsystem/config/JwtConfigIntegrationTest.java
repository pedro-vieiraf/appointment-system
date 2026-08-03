package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedrovieira.appointmentsystem.config.JwtConfig;
import com.pedrovieira.appointmentsystem.config.JwtProperties;
import com.pedrovieira.appointmentsystem.entity.User;
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
  void shouldWireJwtServiceWithConfiguredProperties() {
    assertNotNull(jwtService);

    User user = new User("User Resu", "user@email.com", "superstrongpassword");
    String token = jwtService.generateToken(user);

    assertTrue(jwtService.isValidToken(token, user));
  }
}