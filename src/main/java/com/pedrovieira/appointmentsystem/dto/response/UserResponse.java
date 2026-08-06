package com.pedrovieira.appointmentsystem.dto.response;

import com.pedrovieira.appointmentsystem.entity.User;

/**
 * UserResponse DTO.
 */
public record UserResponse(Long id, String fullName, String email) {

  /**
   * Function that transforms the Entity User in UserResponse.
   */
  public static UserResponse fromUser(User user) {
    return new UserResponse(
        user.getId(),
        user.getFullName(),
        user.getEmail()
    );
  }
}
