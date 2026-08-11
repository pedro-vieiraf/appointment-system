package com.pedrovieira.appointmentsystem.dto.request;

import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * Update Appointment Request.
 */
public record UpdateAppointmentRequest(
    @Size(min = 1, message = "Title cannot be empty.")
    String title,

    String description,

    @Future(message = "Appointment date must be in the future.")
    LocalDateTime appointmentDate
) {

}
