package com.pedrovieira.appointmentsystem.dto.response;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;

/**
 * DTO for Appointment Response.
 */
public record AppointmentResponse(
    Long id,
    String title,
    String description,
    LocalDateTime appointmentDate,
    AppointmentStatus status
) {

  public static AppointmentResponse fromAppointment(
      Appointment appointment
  ) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getTitle(),
        appointment.getDescription(),
        appointment.getAppointmentDate(),
        appointment.getStatus()
    );
  }
}
