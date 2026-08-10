package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.LocalDateTime;

/**
 * DTO for creating appointments.
 */
public record AppointmentRequest(

    @NotBlank(message = "Title is required.")
    String title,

    String description,

    @NotNull(message = "Appointment date is required.")
    @Future(message = "Appointment date must be in the future.")
    LocalDateTime appointmentDate

) {

  public Appointment toEntity(User user) {
    return new Appointment(
        user,
        title,
        description,
        appointmentDate
    );
  }
}