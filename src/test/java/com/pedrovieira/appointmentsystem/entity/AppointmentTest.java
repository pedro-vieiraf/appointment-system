package com.pedrovieira.appointmentsystem.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

@DisplayName("Appointment Entity Tests")
class AppointmentTest {

  @Nested
  @DisplayName("Creation")
  class Creation {

    @Test
    @DisplayName("Should create appointment with pending status by default")
    public void shouldCreateAppointmentWithPendingStatus() {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now().plusDays(1);

      // Act
      Appointment appointment =
          new Appointment(
              user,
              "Medical appointment",
              appointmentDate
          );

      // Assert
      assertEquals(
          AppointmentStatus.PENDING,
          appointment.getStatus()
      );
    }


    @Test
    @DisplayName("Should create appointment with provided data")
    public void shouldCreateAppointmentWithProvidedData() {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.of(
              2026,
              8,
              10,
              10,
              30
          );

      // Act
      Appointment appointment =
          new Appointment(
              user,
              "Dentist",
              appointmentDate
          );
      appointment.setDescription("Dental cleaning");

      // Assert
      assertEquals(user, appointment.getUser());

      assertEquals(
          "Dentist",
          appointment.getTitle()
      );

      assertEquals(
          "Dental cleaning",
          appointment.getDescription()
      );

      assertEquals(
          appointmentDate,
          appointment.getAppointmentDate()
      );
    }
  }


  @Nested
  @DisplayName("Status Management")
  class StatusManagement {

    @Test
    @DisplayName("Should update appointment status")
    public void shouldUpdateAppointmentStatus() {

      // Arrange
      Appointment appointment =
          createAppointment();

      // Act
      appointment.markAsConfirmed();

      // Assert
      assertEquals(
          AppointmentStatus.CONFIRMED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should markAsCanceled an appointment")
    public void shouldCancelAppointment() {
      // Arrange
      Appointment appointment =
          createAppointment();

      // Act
      appointment.markAsCanceled();

      // Assert
      assertEquals(
          AppointmentStatus.CANCELLED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should conclude an appointment")
    public void shouldConcludeAppointment() {
      // Arrange
      Appointment appointment =
          createAppointment();

      // Act
      appointment.markAsCompleted();

      // Assert
      assertEquals(
          AppointmentStatus.COMPLETED,
          appointment.getStatus()
      );
    }
  }

  @Test
  @DisplayName("Should return the correct appointmentDate")
  void shouldReturnAppointmentDate() {
    // Arrange
    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    LocalDateTime appointmentDate =
        LocalDateTime.of(2026, 8, 10, 14, 30);

    Appointment appointment = new Appointment(
        user,
        "Medical appointment",
        appointmentDate
    );

    // Act
    LocalDateTime result = appointment.getAppointmentDate();

    // Assert
    assertEquals(appointmentDate, result);
  }

  private Appointment createAppointment() {

    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    Appointment appointment = new Appointment(
        user,
        "Appointment",
        LocalDateTime.now().plusDays(1)
    );
    appointment.setDescription("Description");

    return appointment;
  }
}