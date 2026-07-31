package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import com.pedrovieira.appointmentsystem.entity.User;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing JwtService functions")
public class JwtServiceTest {

  private JwtService jwtService;

  @Test
  public void shouldGenerateTokenSuccessfully() {
    User user = new User(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    String token = jwtService.generateToken(user);

    assertNotNull(token);
    assertFalse(token.isBlank());
  }

  @Test
  public void shouldExtractNameFromToken() {
    // Arrange
    User user = new User(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    String token = jwtService.generateToken(user);

    // Act
    String email = jwtService.extractUsername(token);

    // Assert
    assertEquals(user.getUsername(), email);
  }

  @Test
  public void shouldReturnFalseForTokenWithDifferentName() {
    // Arrange
    User user1 = new User(
        "User One",
        "user@email.com",
        "superstrongpassword"
    );

    User user2 = new User(
        "User Two",
        "user2@email.com",
        "123456781234"
    );

    String token = jwtService.generateToken(user);

    // Act + Assert
    assertFalse(jwtService.isTokenValid(token, user2));
  }

  @Test
  public void shouldReturnTrueForValidToken() {

  }

  @Test
  public void shouldReturnFalseForExpiredToken() {

  }
}
