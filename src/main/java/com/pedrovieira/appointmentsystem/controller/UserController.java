package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.response.UserResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Users Routes Controller.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/users")
public class UserController {

  /**
   * GET Route that returns the user information from the JWT.
   */
  @GetMapping("/me")
  public ResponseEntity<UserResponse> getCurrentUser(
      @AuthenticationPrincipal User user
  ) {
    return ResponseEntity.ok(UserResponse.fromUser(user));
  }
}

