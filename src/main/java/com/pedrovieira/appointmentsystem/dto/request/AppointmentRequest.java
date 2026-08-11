package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;

/**
 * DTO for creating appointments.
 */
public record AppointmentRequest(

    @NotBlank(message = "Title is required.")
    @Size(
        min = 3,
        max = 50,
        message = "Title must contain between 3 and 50 characters."
    )
    String title,

    @Size(
        min = 3,
        max = 500,
        message = "Description must contain between 3 and 500 characters."
    )
    String description,

    @NotNull(message = "Appointment date is required.")
    @Future(message = "Appointment date must be in the future.")
    LocalDateTime appointmentDate

) {

  /**
   * From DTO to Entity.
   */
  public Appointment toEntity(User user) {
    return new Appointment(
        user,
        title,
        description,
        appointmentDate
    );
  }
}