package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.validation.SizeCheck;
import io.swagger.v3.oas.annotations.media.Schema;
import jakarta.validation.constraints.Future;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import java.time.LocalDateTime;
import java.util.List;

/**
 * DTO for creating appointments.
 */
public record AppointmentRequest(

    @NotBlank(message = "Title is required.")
    @Size(
        min = 3,
        max = 50,
        message = "Title must contain between 3 and 50 characters.",
        groups = SizeCheck.class
    )
    String title,

    @Size(
        min = 3,
        max = 500,
        message = "Description must contain between 3 and 500 characters.",
        groups = SizeCheck.class
    )
    String description,

    @NotNull(message = "Appointment date is required.")
    @Future(message = "Appointment date must be in the future.")
    LocalDateTime appointmentDate,

    @Schema(description = "Unique positive offsets in minutes. Omit for 1440; [] disables.")
    List<Integer> reminderOffsetsMinutes,

    @Min(1) @Max(Appointment.MAX_DURATION_MINUTES)
    @Schema(description = "Duration in minutes. Defaults to 30 when omitted.")
    Integer durationMinutes

) {

  /**
   * Creates an appointment request with the default reminder settings.
   */
  public AppointmentRequest(String title, String description, LocalDateTime appointmentDate) {
    this(title, description, appointmentDate, null, null);
  }


  /** Creates a request with explicit reminders and no duration override. */
  public AppointmentRequest(
      String title, String description, LocalDateTime appointmentDate,
      List<Integer> reminderOffsetsMinutes
  ) {
    this(title, description, appointmentDate, reminderOffsetsMinutes, null);
  }

  /**
   * From DTO to Entity.
   */
  public Appointment toEntity(User user) {
    Appointment appointment = new Appointment(
        user,
        title,
        description,
        appointmentDate
    );
    if (reminderOffsetsMinutes != null) {
      appointment.replaceReminderOffsets(reminderOffsetsMinutes);
    }
    if (durationMinutes != null) {
      appointment.changeDuration(durationMinutes);
    }
    return appointment;
  }
}
