package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentConflictException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentDateInPastException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidAppointmentDateRangeException;
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
import org.mockito.ArgumentMatchers;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;


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
      Appointment response = appointmentService.getById(user, appointment1.getId());

      // Assert
      assertEquals(appointment1, response);

      verify(appointmentRepository).findByIdAndUser(user, appointment1.getId());
    }

    @Test
    @DisplayName("Should not get appointment when appointment is not found")
    void shouldNotGetAppointmentWhenAppointmentIsNotFound() {
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

    @Test
    @DisplayName("Should not get appointment belonging to another user")
    void shouldNotGetAppointmentBelongingToAnotherUser() {
      // Arrange
      User authenticatedUser = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      User appointmentOwner = new User(
          "Jane Doe",
          "jane@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          appointmentOwner,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.setId(1L);

      when(
          appointmentRepository.findByIdAndUser(
              authenticatedUser,
              appointment.getId()
          )
      ).thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.getById(
                  authenticatedUser,
                  appointment.getId()
              )
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(
              authenticatedUser,
              appointment.getId()
          );
    }
  }

  @Nested
  @DisplayName("Search Appointments")
  class SearchAppointmentsTests {

    @Test
    @DisplayName("Should return all appointments when no filters are provided")
    void shouldReturnAllAppointmentsWhenNoFiltersAreProvided() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(2)
      );

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any()
          )
      ).thenReturn(appointments);

      Pageable pageable =
          PageRequest.of(0, 10);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable
          );

      // Assert
      assertEquals(2, result.getContent().size());
      assertEquals(appointments, result);

      verify(
          appointmentRepository
      ).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by title")
    void shouldFilterAppointmentsByTitle() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(2)
      );

      List<Appointment> appointments = List.of(
          appointment1
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointment1, result.getContent().get(0));

      verify(appointmentRepository).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by start date")
    void shouldFilterAppointmentsByStartDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(4);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(2)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(5)
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              null
          );

      List<Appointment> appointments = List.of(
          appointment2
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointments, result);

      verify(appointmentRepository).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by end date")
    void shouldFilterAppointmentsByEndDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(4);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(2)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(5)
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              endDate
          );

      List<Appointment> appointments = List.of(
          appointment1
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointments, result);

      verify(appointmentRepository).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by date range")
    void shouldFilterAppointmentsByDateRange() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(4);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(3)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(5)
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              endDate
          );

      List<Appointment> appointments = List.of(
          appointment1
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointments, result);

      verify(appointmentRepository).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by title and start date")
    void shouldFilterAppointmentsByTitleAndStartDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              startDate,
              null
          );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(3)
      );

      List<Appointment> appointments = List.of(
          appointment
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointment, result.getContent().get(0));

      verify(
          appointmentRepository
      ).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by title and end date")
    void shouldFilterAppointmentsByTitleAndEndDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(4);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              null,
              endDate
          );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(3)
      );

      List<Appointment> appointments = List.of(
          appointment
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointment, result.getContent().get(0));

      verify(
          appointmentRepository
      ).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should filter appointments by title and date range")
    void shouldFilterAppointmentsByTitleAndDateRange() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(5);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              startDate,
              endDate
          );

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(3)
      );

      List<Appointment> appointments = List.of(
          appointment
      );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              ArgumentMatchers.<Specification<Appointment>>any(),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable);

      // Assert
      assertEquals(1, result.getContent().size());
      assertEquals(appointment, result.getContent().get(0));

      verify(
          appointmentRepository
      ).findAll(
          ArgumentMatchers.<Specification<Appointment>>any()
      );
    }

    @Test
    @DisplayName("Should throw exception when end date is before start date")
    void shouldThrowExceptionWhenEndDateIsBeforeStartDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now =
          LocalDateTime.now().withNano(0);

      LocalDateTime startDate =
          now.plusDays(5);

      LocalDateTime endDate =
          now.plusDays(2);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              endDate
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      // Act + Assert
      assertThrows(
          InvalidAppointmentDateRangeException.class,
          () -> appointmentService.search(
              user,
              criteria,
              pageable
          )
      );

      verifyNoInteractions(appointmentRepository);
    }

    @Test
    @DisplayName("Should return paginated appointments")
    void shouldReturnPaginatedAppointments() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(2)
      );

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(0, 2);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentRepository.findAll(
              any(Specification.class),
              eq(pageable)
          )
      ).thenReturn(page);

      // Act
      Page<Appointment> result =
          appointmentService.search(
              user,
              criteria,
              pageable
          );

      // Assert
      assertEquals(2, result.getContent().size());
      assertEquals(2, result.getTotalElements());
      assertEquals(1, result.getTotalPages());
      assertEquals(0, result.getNumber());
      assertEquals(2, result.getSize());

      assertEquals(
          appointments,
          result.getContent()
      );

      verify(appointmentRepository)
          .findAll(
              any(Specification.class),
              eq(pageable)
          );
    }
  }

  @Nested
  @DisplayName("Update Appointments")
  class UpdateAppointmentsTests {

    @Test
    @DisplayName("Should update only the title")
    void shouldUpdateOnlyTheProvidedFields() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime originalDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          originalDate
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "Updated appointment",
              null,
              null
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(appointmentRepository.save(appointment))
          .thenReturn(appointment);

      // Act
      Appointment response =
          appointmentService.update(
              user,
              appointment.getId(),
              request
          );

      // Assert
      assertEquals(
          "Updated appointment",
          response.getTitle()
      );

      assertEquals(
          "Annual checkup",
          response.getDescription()
      );

      assertEquals(
          originalDate,
          response.getAppointmentDate()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointment.getId());

      verify(appointmentRepository)
          .save(appointment);
    }

    @Test
    @DisplayName("Should update only the description")
    void shouldUpdateOnlyTheDescription() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime originalDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          originalDate
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              "Updated description",
              null
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(appointmentRepository.save(appointment))
          .thenReturn(appointment);

      // Act
      Appointment response =
          appointmentService.update(
              user,
              appointment.getId(),
              request
          );

      // Assert
      assertEquals(
          "Medical appointment",
          response.getTitle()
      );

      assertEquals(
          "Updated description",
          response.getDescription()
      );

      assertEquals(
          originalDate,
          response.getAppointmentDate()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointment.getId());

      verify(appointmentRepository)
          .save(appointment);
    }

    @Test
    @DisplayName("Should update only the appointment date")
    void shouldUpdateOnlyTheAppointmentDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime originalDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          originalDate
      );

      appointment.setId(1L);

      LocalDateTime updatedDate =
          LocalDateTime.now()
              .plusDays(2)
              .withNano(0);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              null,
              updatedDate
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(appointmentRepository.save(appointment))
          .thenReturn(appointment);

      // Act
      Appointment response =
          appointmentService.update(
              user,
              appointment.getId(),
              request
          );

      // Assert
      assertEquals(
          "Medical appointment",
          response.getTitle()
      );

      assertEquals(
          "Annual checkup",
          response.getDescription()
      );

      assertEquals(
          updatedDate,
          response.getAppointmentDate()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointment.getId());

      verify(appointmentRepository)
          .save(appointment);
    }

    @Test
    @DisplayName("Should throw exception when appointment is not found")
    void shouldThrowExceptionWhenAppointmentIsNotFound() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "Updated appointment",
              null,
              null
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointmentId
      )).thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.update(
                  user,
                  appointmentId,
                  request
              )
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointmentId);

      verify(appointmentRepository, never())
          .save(any());
    }

    @Test
    @DisplayName("Should not update appointment belonging to another user")
    void shouldNotUpdateAppointmentBelongingToAnotherUser() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "Updated appointment",
              null,
              null
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointmentId
      )).thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.update(
                  user,
                  appointmentId,
                  request
              )
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointmentId);

      verify(appointmentRepository, never())
          .save(any());
    }

    @Test
    @DisplayName("Should save the updated appointment")
    void shouldSaveTheUpdatedAppointment() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Old title",
          "Old description",
          LocalDateTime.now().plusDays(1)
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "New title",
              "New description",
              appointment.getAppointmentDate()
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(appointmentRepository.save(appointment))
          .thenReturn(appointment);

      // Act
      Appointment response = appointmentService.update(
          user,
          appointment.getId(),
          request
      );

      // Assert
      assertEquals("New title", response.getTitle());
      assertEquals("New description", response.getDescription());

      verify(appointmentRepository).save(appointment);
    }

    @Test
    @DisplayName("Should reject appointment when the new date conflicts")
    void shouldRejectAppointmentWhenNewDateConflicts() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime oldDate =
          LocalDateTime.now().plusDays(1);

      LocalDateTime conflictingDate =
          LocalDateTime.now().plusDays(2);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          oldDate
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              null,
              conflictingDate
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(appointmentRepository.existsByUserAndAppointmentDateAndIdNot(
          user,
          request.appointmentDate(),
          appointment.getId()
      )).thenReturn(true);

      // Act + Assert
      AppointmentConflictException exception =
          assertThrows(
              AppointmentConflictException.class,
              () -> appointmentService.update(
                  user,
                  appointment.getId(),
                  request
              )
          );

      assertEquals(
          "Appointment date conflicts with another appointment.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .existsByUserAndAppointmentDateAndIdNot(user, request.appointmentDate(),
              appointment.getId());

      verify(appointmentRepository, never())
          .save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should reject appointment when the new date is in the past")
    void shouldRejectAppointmentWhenNewDateIsInThePast() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime currentDate =
          LocalDateTime.now().plusDays(1);

      LocalDateTime pastDate =
          LocalDateTime.now().minusDays(1);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          currentDate
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              null,
              pastDate
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      // Act + Assert
      AppointmentDateInPastException exception =
          assertThrows(
              AppointmentDateInPastException.class,
              () -> appointmentService.update(
                  user,
                  appointment.getId(),
                  request
              )
          );

      assertEquals(
          "Appointment date must be in the future.",
          exception.getMessage()
      );

      verify(appointmentRepository, never())
          .save(any(Appointment.class));
    }

    @Test
    @DisplayName("Should update appointment when new date is the same as its current date")
    void shouldUpdateAppointmentWhenNewDateIsTheSameAsItsCurrentDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now().plusDays(1).withNano(0);

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          appointmentDate
      );

      appointment.setId(1L);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "Updated appointment",
              null,
              appointmentDate
          );

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      when(
          appointmentRepository.existsByUserAndAppointmentDateAndIdNot(
              user,
              appointmentDate,
              appointment.getId()
          )
      ).thenReturn(false);

      when(appointmentRepository.save(appointment))
          .thenReturn(appointment);

      // Act
      Appointment response = appointmentService.update(
          user,
          appointment.getId(),
          request
      );

      // Assert
      assertEquals("Updated appointment", response.getTitle());
      assertEquals("Annual checkup", response.getDescription());
      assertEquals(appointmentDate, response.getAppointmentDate());

      verify(appointmentRepository)
          .findByIdAndUser(user, appointment.getId());

      verify(appointmentRepository)
          .existsByUserAndAppointmentDateAndIdNot(
              user,
              appointmentDate,
              appointment.getId()
          );

      verify(appointmentRepository)
          .save(appointment);
    }
  }

  @Nested
  @DisplayName("Delete Appointment")
  class DeleteAppointmentTests {

    @Test
    @DisplayName("Should delete appointment successfully")
    void shouldDeleteAppointmentSuccessfully() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment.setId(1L);

      when(appointmentRepository.findByIdAndUser(
          user,
          appointment.getId()
      )).thenReturn(Optional.of(appointment));

      // Act
      appointmentService.delete(user, appointment.getId());

      // Assert
      verify(appointmentRepository)
          .findByIdAndUser(user, appointment.getId());

      verify(appointmentRepository)
          .delete(appointment);
    }

    @Test
    @DisplayName("Should return 404 exception if appointment is not found")
    void shouldReturn404ExceptionIfAppointmentIsNotFound() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      when(appointmentRepository.findByIdAndUser(user, appointmentId))
          .thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.delete(
                  user,
                  appointmentId
              )
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointmentId);

      verify(appointmentRepository, never())
          .delete(any(Appointment.class));
    }

    @Test
    @DisplayName("Should not delete appointment belonging to another user")
    void shouldNotDeleteAppointmentBelongingToAnotherUser() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      when(appointmentRepository.findByIdAndUser(user, appointmentId))
          .thenReturn(Optional.empty());

      // Act + Assert
      AppointmentNotFoundException exception =
          assertThrows(
              AppointmentNotFoundException.class,
              () -> appointmentService.delete(
                  user,
                  appointmentId
              )
          );

      assertEquals(
          "Appointment not found.",
          exception.getMessage()
      );

      verify(appointmentRepository)
          .findByIdAndUser(user, appointmentId);

      verify(appointmentRepository, never())
          .delete(any(Appointment.class));
    }
  }
}