package com.pedrovieira.appointmentsystem.dto.response;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pedrovieira.appointmentsystem.entity.User;
import org.junit.jupiter.api.Test;

public class UserResponseTest {

  @Test
  void shouldCreateUserResponseFromUser() {
    // Arrange
    User user = new User(
        "User Resu",
        "user@mail.com",
        "superstrongpassword");

    user.setId(1L);

    // Act
    UserResponse response = UserResponse.fromUser(user);

    // Assert
    assertEquals(1L, response.id());
    assertEquals("User Resu", response.fullName());
    assertEquals("user@mail.com", response.email());
  }
}