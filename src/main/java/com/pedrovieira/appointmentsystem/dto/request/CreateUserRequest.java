package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;

public record CreateUserRequest(
    String fullName,

    @NotBlank
    @Email
    String email,

    String password
) {
}
