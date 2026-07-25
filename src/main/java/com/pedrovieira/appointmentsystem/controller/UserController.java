package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.dto.response.UserResponse;
import com.pedrovieira.appointmentsystem.exception.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.service.UserService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that will handle users.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "auth")
public class UserController {

  private final UserService userService;

  @PostMapping
  public UserResponse createUser(@Valid @RequestBody CreateUserRequest request)
      throws EmailAlreadyExistsException {

  }
}
