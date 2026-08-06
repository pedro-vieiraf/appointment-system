package com.pedrovieira.appointmentsystem.security;

import com.pedrovieira.appointmentsystem.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Duration;
import java.util.Date;
import javax.crypto.SecretKey;

/**
 * Service to handle JWT Token.
 */
public class JwtService {

  private final SecretKey secretKey;
  private final Duration expirationTime;
  private final Clock clock;

  /**
   * Creates a JwtService using the system UTC clock.
   */
  public JwtService(String secret, Duration expirationTime) {
    this(secret, expirationTime, Clock.systemUTC());
  }

  /**
   * Creates a JwtService with an explicit clock, allowing tests to control the current time when
   * generating and validating tokens.
   */
  public JwtService(String secret, Duration expirationTime, Clock clock) {
    this.secretKey = Keys.hmacShaKeyFor(
        secret.getBytes(StandardCharsets.UTF_8)
    );
    this.expirationTime = expirationTime;
    this.clock = clock;
  }

  /**
   * Function to generate a new Token.
   */
  public String generateToken(User user) {
    Date now = Date.from(clock.instant());
    Date expiration = new Date(now.getTime() + expirationTime.toMillis());

    return Jwts.builder()
        .subject(String.valueOf(user.getId()))
        .issuedAt(now)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Function to extract userId from Token.
   */
  public Long extractUserId(String token) {
    return Long.valueOf(extractAllClaims(token).getSubject());
  }

  /**
   * Function to validate if token is valid.
   */
  public boolean belongsToUser(String token, User user) {
    try {
      Long userId = extractUserId(token);
      return userId.equals(user.getId());
    } catch (JwtException e) {
      return false;
    }
  }

  /**
   * Function to validate if token is valid without User.
   */
  public boolean isValidToken(String token) {
    try {
      extractAllClaims(token);
      return true;
    } catch (JwtException | IllegalArgumentException e) {
      return false;
    }
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .clock(() -> Date.from(clock.instant()))
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public long getExpirationInSeconds() {
    return expirationTime.getSeconds();
  }
}
