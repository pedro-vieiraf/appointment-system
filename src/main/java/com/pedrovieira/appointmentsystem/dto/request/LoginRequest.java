package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

/**
 * Login Request DTO.
 */
public record LoginRequest(
    @NotBlank
    @Email
    String email,

    @NotBlank
    String password
) {

}
