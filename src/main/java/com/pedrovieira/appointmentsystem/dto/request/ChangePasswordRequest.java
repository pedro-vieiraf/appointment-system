package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.validation.SizeCheck;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

/**
 * Update password DTO.
 */
public record ChangePasswordRequest(

    @NotBlank(message = "Current password is required.")
    @Size(min = 8, message = "Current password must have at least 8 characters.", groups = SizeCheck.class)
    String currentPassword,

    @NotBlank(message = "New password is required.")
    @Size(min = 8, message = "New password must have at least 8 characters.", groups = SizeCheck.class)
    String newPassword
) {

}
