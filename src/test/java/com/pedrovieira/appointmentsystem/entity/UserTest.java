package com.pedrovieira.appointmentsystem.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import java.util.Collection;

import org.junit.jupiter.api.Test;
import org.springframework.security.core.GrantedAuthority;

class UserTest {

  @Test
  void shouldCreateUserWithProvidedData() {
    // Arrange
    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    // Assert
    assertEquals(
        "John Doe",
        user.getFullName()
    );

    assertEquals(
        "john@email.com",
        user.getEmail()
    );

    assertEquals(
        "hashed-password",
        user.getPassword()
    );
  }


  @Test
  void shouldSetAndGetUserId() {
    // Arrange
    User user = new User();

    // Act
    user.setId(1L);

    // Assert
    assertEquals(
        1L,
        user.getId()
    );
  }


  @Test
  void shouldReturnEmailAsUsernameForSpringSecurity() {
    // Arrange
    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    // Assert
    assertEquals(
        "john@email.com",
        user.getUsername()
    );
  }


  @Test
  void shouldReturnPasswordForSpringSecurity() {
    // Arrange
    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    // Assert
    assertEquals(
        "hashed-password",
        user.getPassword()
    );
  }


  @Test
  void shouldReturnEmptyAuthoritiesWhenUserHasNoRoles() {
    // Arrange
    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    // Act
    Collection<? extends GrantedAuthority> authorities =
        user.getAuthorities();

    // Assert
    assertNotNull(authorities);
    assertEquals(
        0,
        authorities.size()
    );
  }
}