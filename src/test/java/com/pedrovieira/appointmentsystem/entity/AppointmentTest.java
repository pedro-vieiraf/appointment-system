package com.pedrovieira.appointmentsystem.entity;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.NullAndEmptySource;
import org.junit.jupiter.params.provider.ValueSource;

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

    @ParameterizedTest
    @NullAndEmptySource
    @ValueSource(strings = "   ")
    @DisplayName("Should reject an appointment without a title")
    void shouldRejectAppointmentWithoutTitle(String title) {
      User user = new User("John Doe", "john@email.com", "hashed-password");
      LocalDateTime appointmentDate = LocalDateTime.of(2026, 8, 10, 10, 30);

      assertThrows(IllegalArgumentException.class,
          () -> new Appointment(user, title, appointmentDate));
    }

    @Test
    @DisplayName("Should reject an appointment without a date")
    void shouldRejectAppointmentWithoutDate() {
      User user = new User("John Doe", "john@email.com", "hashed-password");

      assertThrows(IllegalArgumentException.class,
          () -> new Appointment(user, "Dentist", null));
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
      appointment.markAsConfirmed();
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
      appointment.markAsConfirmed();
      appointment.markAsCompleted();

      // Assert
      assertEquals(
          AppointmentStatus.COMPLETED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should cancel a pending appointment")
    void shouldCancelPendingAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      // Act
      appointment.markAsCanceled();

      // Assert
      assertEquals(
          AppointmentStatus.CANCELLED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not complete a pending appointment")
    void shouldNotCompletePendingAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      // Act + Assert
      BusinessRuleException exception = assertThrows(
          BusinessRuleException.class,
          appointment::markAsCompleted
      );

      assertEquals(
          "Only confirmed appointments can be completed",
          exception.getMessage()
      );

      assertEquals(
          AppointmentStatus.PENDING,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not confirm an already confirmed appointment")
    void shouldNotConfirmAlreadyConfirmedAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.markAsConfirmed();

      // Act + Assert
      BusinessRuleException exception = assertThrows(
          BusinessRuleException.class,
          appointment::markAsConfirmed
      );

      assertEquals(
          "Only pending appointments can be confirmed",
          exception.getMessage()
      );

      assertEquals(
          AppointmentStatus.CONFIRMED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not cancel an already cancelled appointment")
    void shouldNotCancelAlreadyCancelledAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.markAsConfirmed();
      appointment.markAsCanceled();

      // Act + Assert
      BusinessRuleException exception = assertThrows(
          BusinessRuleException.class,
          appointment::markAsCanceled
      );

      assertEquals(
          "Only pending or confirmed appointments can be cancelled",
          exception.getMessage()
      );

      assertEquals(
          AppointmentStatus.CANCELLED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not confirm a cancelled appointment")
    void shouldNotConfirmCancelledAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.markAsConfirmed();
      appointment.markAsCanceled();

      // Act + Assert
      BusinessRuleException exception = assertThrows(
          BusinessRuleException.class,
          appointment::markAsConfirmed
      );

      assertEquals(
          "Only pending appointments can be confirmed",
          exception.getMessage()
      );

      assertEquals(
          AppointmentStatus.CANCELLED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not complete a cancelled appointment")
    void shouldNotCompleteCancelledAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.markAsConfirmed();
      appointment.markAsCanceled();

      // Act + Assert
      BusinessRuleException exception = assertThrows(
          BusinessRuleException.class,
          appointment::markAsCompleted
      );

      assertEquals(
          "Only confirmed appointments can be completed",
          exception.getMessage()
      );

      assertEquals(
          AppointmentStatus.CANCELLED,
          appointment.getStatus()
      );
    }

    @Test
    @DisplayName("Should not change a completed appointment status")
    void shouldNotChangeCompletedAppointmentStatus() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.markAsConfirmed();
      appointment.markAsCompleted();

      // Act + Assert
      assertThrows(
          BusinessRuleException.class,
          appointment::markAsConfirmed
      );

      assertThrows(
          BusinessRuleException.class,
          appointment::markAsCanceled
      );

      assertThrows(
          BusinessRuleException.class,
          appointment::markAsCompleted
      );

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