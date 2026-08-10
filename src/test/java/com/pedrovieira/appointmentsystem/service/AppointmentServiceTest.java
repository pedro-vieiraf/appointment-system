package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import java.time.Clock;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;


@ExtendWith(MockitoExtension.class)
@DisplayName("Appointment Service Tests")
class AppointmentServiceTest {

  @InjectMocks
  private AppointmentService appointmentService;

  @Mock
  private AppointmentRepository appointmentRepository;

  @Mock
  private Clock clock;

  @BeforeEach()
  void setup() {

    clock = Clock.fixed(
        Instant.parse("2026-08-01T10:00:00Z"),
        ZoneOffset.UTC
    );

    appointmentService =
        new AppointmentService(
            appointmentRepository,
            clock
        );
  }

  @Nested
  @DisplayName("Create Appointment")
  class CreateAppointment {

    @Test
    @DisplayName("Should create appointment successfully")
    void shouldCreateAppointmentSuccessfully() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      Appointment savedAppointment = new Appointment(
          user,
          request.title(),
          request.description(),
          request.appointmentDate()
      );

      savedAppointment.setId(1L);

      when(appointmentRepository.save(any(Appointment.class)))
          .thenReturn(savedAppointment);

      // Act
      Appointment response =
          appointmentService.create(user, request);

      // Assert
      assertNotNull(response);

      assertEquals(
          savedAppointment.getId(),
          response.getId()
      );

      assertEquals(
          request.title(),
          response.getTitle()
      );

      assertEquals(
          request.appointmentDate(),
          response.getAppointmentDate()
      );

      assertEquals(
          AppointmentStatus.PENDING,
          response.getStatus()
      );

      verify(appointmentRepository)
          .save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should create appointment with empty description when description is null")
    void shouldCreateAppointmentWithEmptyDescriptionWhenDescriptionIsNull() {

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          null,
          LocalDateTime.now(clock).plusDays(1)
      );

      Appointment savedAppointment = new Appointment(
          user,
          request.title(),
          "",
          request.appointmentDate()
      );

      when(appointmentRepository.save(any(Appointment.class)))
          .thenReturn(savedAppointment);

      Appointment response =
          appointmentService.create(user, request);

      assertEquals(
          "",
          response.getDescription()
      );
    }


    @Test
    @DisplayName("Should not create appointment with past date")
    void shouldNotCreateAppointmentWithPastDate() {

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now(clock).minusDays(1)
      );

      BusinessRuleException exception =
          assertThrows(
              BusinessRuleException.class,
              () -> appointmentService.create(user, request)
          );

      assertEquals(
          "Appointment date is before current date",
          exception.getMessage()
      );

      verify(appointmentRepository, never())
          .save(any());
    }


    @Test
    @DisplayName("Should not create appointment in an occupied time")
    void shouldNotCreateAppointmentWhenTimeIsAlreadyBooked() {

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      when(
          appointmentRepository.existsByUserAndAppointmentDate(
              user,
              request.appointmentDate()
          )
      )
          .thenReturn(true);

      BusinessRuleException exception =
          assertThrows(
              BusinessRuleException.class,
              () -> appointmentService.create(user, request)
          );

      assertEquals(
          "User already has an appointment at this time",
          exception.getMessage()
      );

      verify(appointmentRepository, never())
          .save(any());
    }
  }

  @Nested
  @DisplayName("Get All Appointments")
  class GetAllAppointmentsTests {

    @Test
    @DisplayName("Get all appointments successfully")
    void getAllAppointmentsSuccessfully() {
      // Assert
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental cleaning",
          LocalDateTime.now().plusDays(2)
      );

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );
      when(appointmentRepository.findAllByUser(user))
          .thenReturn(appointments);

      // Act
      List<Appointment> response = appointmentService.getAll(user);

      // Assert
      assertEquals(2, response.size());
      assertEquals(appointments, response);

      verify(appointmentRepository).findAllByUser(user);
    }

    @Test
    @DisplayName("Get empty list when user has no appointments")
    void getAllAppointmentsWhenUserHasNoAppointments() {
      // Assert
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      List<Appointment> appointments = List.of();

      when(appointmentRepository.findAllByUser(user))
          .thenReturn(appointments);

      // Act
      List<Appointment> response = appointmentService.getAll(user);

      // Assert
      assertEquals(0, response.size());

      verify(appointmentRepository).findAllByUser(user);
    }

  }

  @Nested
  @DisplayName("Get Appointments By ID")
  class GetAppointmentsByIdTests {

    @Test
    @DisplayName("Get appointment by ID")
    void getAppointmentByIdSuccessfully() {
      // Assert
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment1.setId(1L);

      when(appointmentRepository.findByIdAndUser(user, appointment1.getId()))
          .thenReturn(Optional.of(appointment1));

      // Act
      Optional<Appointment> response = appointmentService.getById(user, appointment1.getId());

      // Assert
      assertEquals(Optional.of(appointment1), response);

      verify(appointmentRepository).findByIdAndUser(user, appointment1.getId());
    }

    @Test
    @DisplayName("Should not get appointment belonging to another user")
    void shouldNotGetAppointmentBelongingToAnotherUser() {
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment1.setId(1L);

      when(appointmentRepository.findByIdAndUser(user, appointment1.getId()))
          .thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.getById(user, appointment1.getId())
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointment1.getId());
    }
  }
}