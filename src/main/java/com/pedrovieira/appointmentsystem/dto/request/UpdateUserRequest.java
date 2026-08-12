package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.Size;

public record UpdateUserRequest(

    @Size(min = 3, max = 50,
        message = "Full name must contain between 3 and 50 characters.")
    String fullName,

    @Email(message = "Email must be valid.")
    String email
) {}