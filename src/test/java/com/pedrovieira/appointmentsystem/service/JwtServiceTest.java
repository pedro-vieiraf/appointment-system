package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

import com.pedrovieira.appointmentsystem.entity.User;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.ZoneOffset;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

@DisplayName("Testing JwtService functions")
public class JwtServiceTest {

  private static final String TEST_SECRET =
      "01234567890123456789012345678901234567890123456789";
  private JwtService jwtService;

  @BeforeEach
  public void setUp() {
    jwtService = new JwtService(TEST_SECRET, Duration.ofMinutes(15));
  }

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

    user.setId(1L);
    
    String token = jwtService.generateToken(user);

    // Act
    Long userId = jwtService.extractUserId(token);

    // Assert
    assertEquals(user.getId(), userId);
  }

  @Test
  public void shouldReturnFalseForTokenWithDifferentName() {
    // Arrange
    User user1 = new User(
        "User One",
        "user@email.com",
        "superstrongpassword"
    );

    user1.setId(1L);
    User user2 = new User(
        "User Two",
        "user2@email.com",
        "123456781234"
    );
    user1.setId(2L);

    String token = jwtService.generateToken(user1);

    // Act + Assert
    assertFalse(jwtService.isValidToken(token, user2));
  }

  @Test
  public void shouldValidateTokenGeneratedForSameUser() {
    // Arrange
    User user = new User(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    user.setId(1L);

    String token = jwtService.generateToken(user);

    // Act + Assert
    assertTrue(jwtService.isValidToken(token, user));
  }

  @Test
  public void shouldReturnFalseForExpiredToken() {
    // Arrange
    Clock pastClock = Clock.fixed(
        Instant.now().minus(Duration.ofMinutes(20)),
        ZoneOffset.UTC
    );
    JwtService serviceWithPastClock = new JwtService(TEST_SECRET, Duration.ofMinutes(15),
        pastClock);

    User user = new User(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    String token = serviceWithPastClock.generateToken(user);

    // Act + Assert
    assertFalse(jwtService.isValidToken(token, user));
  }
}
