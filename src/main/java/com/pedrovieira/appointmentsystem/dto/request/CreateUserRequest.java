package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.validation.SizeCheck;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Create user response DTO.
 */
public record CreateUserRequest(
    @NotBlank(message = "Full name is required.")
    @Size(min = 3, max = 50,
        message = "Full name must contain between 2 and 100 characters.", groups = SizeCheck.class)
    String fullName,

    @NotBlank(message = "Email is required.")
    @Email(message = "Email must be valid.")
    String email,

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must have at least 8 characters.", groups = SizeCheck.class)
    String password
) {

}