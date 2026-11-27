package com.pedrovieira.appointmentsystem.dto.response;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import io.swagger.v3.oas.annotations.media.Schema;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;

/**
 * DTO for Appointment Response.
 */
public record AppointmentResponse(
    Long id,
    String title,
    String description,
    @Schema(description = "UTC timestamp without offset. Use appointmentStart for display.")
    LocalDateTime appointmentDate,
    @Schema(description = "Start time in the booking time zone, including its UTC offset.",
        format = "date-time")
    String appointmentStart,
    String appointmentTimeZone,
    AppointmentStatus status,
    List<Integer> reminderOffsetsMinutes,
    int durationMinutes,
    String guestName,
    String guestEmail,
    LocalDateTime holdExpiresAt
) {

  /**
   * From appointment to DTO.
   */
  public static AppointmentResponse fromAppointment(
      Appointment appointment
  ) {
    String timeZone = appointment.getBookingTimeZone();
    String start = appointment.getAppointmentDate().toInstant(ZoneOffset.UTC)
        .atZone(ZoneId.of(timeZone)).format(DateTimeFormatter.ISO_OFFSET_DATE_TIME);
    return new AppointmentResponse(
        appointment.getId(),
        appointment.getTitle(),
        appointment.getDescription(),
        appointment.getAppointmentDate(),
        start,
        timeZone,
        appointment.getStatus(),
        appointment.getReminderOffsetsMinutes(),
        appointment.getDurationMinutes(),
        appointment.getGuestName(),
        appointment.getGuestEmail(),
        appointment.getHoldExpiresAt()
    );
  }
}
