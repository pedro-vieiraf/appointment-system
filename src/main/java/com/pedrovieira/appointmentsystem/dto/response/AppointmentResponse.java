package com.pedrovieira.appointmentsystem.dto.response;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for Appointment Response.
 */
public record AppointmentResponse(
    Long id,
    String title,
    String description,
    LocalDateTime appointmentDate,
    AppointmentStatus status,
    List<Integer> reminderOffsetsMinutes,
    int durationMinutes
) {

  /**
   * From appointment to DTO.
   */
  public static AppointmentResponse fromAppointment(
      Appointment appointment
  ) {
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getTitle(),
        appointment.getDescription(),
        appointment.getAppointmentDate(),
        appointment.getStatus(),
        appointment.getReminderOffsetsMinutes(),
        appointment.getDurationMinutes()
    );
  }
}
