package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.entity.User;
import io.jsonwebtoken.Claims;
import io.jsonwebtoken.JwtException;
import io.jsonwebtoken.Jwts;
import io.jsonwebtoken.security.Keys;
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
    this.secretKey = Keys.hmacShaKeyFor(secret.getBytes());
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
        .subject(user.getUsername())
        .issuedAt(now)
        .expiration(expiration)
        .signWith(secretKey)
        .compact();
  }

  /**
   * Function to extract username from Token.
   */
  public String extractUsername(String token) {
    return extractAllClaims(token).getSubject();
  }

  /**
   * Function to validate if token is valid.
   */
  public boolean isValidToken(String token, User user) {
    try {
      String username = extractUsername(token);
      return username.equals(user.getUsername());
    } catch (JwtException e) {
      return false;
    }
  }

  private boolean isTokenExpired(String token) {
    return extractAllClaims(token).getExpiration().before(new Date());
  }

  private Claims extractAllClaims(String token) {
    return Jwts.parser()
        .verifyWith(secretKey)
        .build()
        .parseSignedClaims(token)
        .getPayload();
  }

  public long getExpirationInSeconds() {
    return expirationTime.getSeconds();
  }
}
