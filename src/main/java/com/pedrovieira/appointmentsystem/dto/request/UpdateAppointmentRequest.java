package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Update Appointment Request.
 */
public record UpdateAppointmentRequest(
    @Size(min = 3, max = 50, message = "Title must contain between 3 and 50 characters.")
    String title,

    @Size(
        min = 3,
        max = 500,
        message = "Description must contain between 3 and 500 characters."
    )
    String description,

    @Future(message = "Appointment date must be in the future.")
    LocalDateTime appointmentDate
) {

}
