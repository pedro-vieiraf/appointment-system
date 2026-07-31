package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.dto.response.UserResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that will handle users.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth")
public class UserController {

  private final UserService userService;

  /**
   * Post Route for creating new User.
   */
  @PostMapping("/register")
  public ResponseEntity<UserResponse> createUser(@Valid @RequestBody CreateUserRequest request) {
    User newUser = userService.createUser(request);

    UserResponse response = new UserResponse(
        newUser.getId(),
        newUser.getFullName(),
        newUser.getEmail()
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response);
  }
}
