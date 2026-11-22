package com.pedrovieira.appointmentsystem.dto.request;

import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

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
    LocalDateTime appointmentDate,

    @Schema(description = "Unique positive offsets in minutes. Omit to keep them; [] disables.")
    List<Integer> reminderOffsetsMinutes
) {

  /**
   * Updates appointment details while keeping the current reminder settings.
   */
  public UpdateAppointmentRequest(String title, String description, LocalDateTime appointmentDate) {
    this(title, description, appointmentDate, null);
  }
}
