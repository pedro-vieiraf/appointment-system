package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.validation.SizeCheck;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update password DTO.
 */
public record ChangePasswordRequest(

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must have at least 8 characters.", groups = SizeCheck.class)
    String currentPassword,

    @NotBlank(message = "Password is required.")
    @Size(min = 8, message = "Password must have at least 8 characters.", groups = SizeCheck.class)
    String newPassword
) {

}
