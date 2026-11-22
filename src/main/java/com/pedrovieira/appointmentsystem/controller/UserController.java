package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.ChangePasswordRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateUserRequest;
import com.pedrovieira.appointmentsystem.dto.response.UserResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.ErrorResponse;
import com.pedrovieira.appointmentsystem.service.UserService;
import com.pedrovieira.appointmentsystem.validation.OrderedValidation;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Users Routes Controller.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping(value = "/users")
public class UserController {

  private final UserService userService;

  /**
   * GET Route that returns the user information from the JWT.
   */
  @GetMapping("/me")
  @ApiResponse(responseCode = "200", description = "Authenticated user profile",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  public ResponseEntity<UserResponse> getCurrentUser(
      @AuthenticationPrincipal User user
  ) {
    return ResponseEntity.ok(UserResponse.fromUser(user));
  }

  /**
   * PATCH Route to update user.
   */
  @PatchMapping("/me")
  @ApiResponse(responseCode = "200", description = "Profile updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid or malformed request body",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "409", description = "Email address is already registered",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<UserResponse> updateUser(
      Authentication authentication,
      @Valid @RequestBody UpdateUserRequest request
  ) {
    User user = (User) authentication.getPrincipal();

    User updatedUser = userService.update(user, request);

    return ResponseEntity.ok(UserResponse.fromUser(updatedUser));
  }

  /**
   * PATCH Route to update password.
   */
  @PatchMapping("/me/password")
  @ApiResponse(responseCode = "204", description = "Password changed",
      content = @Content)
  @ApiResponse(responseCode = "400", description = "Invalid or malformed request body",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "401", description = "Current password is incorrect",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  public ResponseEntity<Void> updatePassword(
      Authentication authentication,
      @Validated(OrderedValidation.class)
      @RequestBody ChangePasswordRequest request
  ) {
    User user = (User) authentication.getPrincipal();

    userService.updatePassword(user, request);

    return ResponseEntity.noContent().build();
  }
}
