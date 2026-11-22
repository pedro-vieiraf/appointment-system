package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import com.pedrovieira.appointmentsystem.dto.response.RegisterResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.ErrorResponse;
import com.pedrovieira.appointmentsystem.service.AuthenticationService;
import com.pedrovieira.appointmentsystem.validation.OrderedValidation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that will handle users.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/auth")
public class AuthController {

  private final AuthenticationService authenticationService;

  /**
   * Post Route for creating new User.
   */
  @PostMapping("/register")
  @ApiResponse(responseCode = "201", description = "Account created",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid or malformed request body",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409", description = "Email address is already registered",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<RegisterResponse> register(
      @Validated(OrderedValidation.class)
      @RequestBody RegisterRequest request) {
    User newUser = authenticationService.register(request);

    RegisterResponse response = new RegisterResponse(
        newUser.getId(),
        newUser.getFullName(),
        newUser.getEmail()
    );

    return ResponseEntity.status(HttpStatus.CREATED)
        .body(response);
  }

  /**
   * Post Route for login.
   */
  @PostMapping("/login")
  @ApiResponse(responseCode = "200", description = "JWT issued",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid or malformed request body",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "401", description = "Invalid credentials",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<LoginResponse> login(
      @Valid @RequestBody LoginRequest request
  ) {
    LoginResponse response = authenticationService.login(request);
    return ResponseEntity.ok(response);
  }
}
