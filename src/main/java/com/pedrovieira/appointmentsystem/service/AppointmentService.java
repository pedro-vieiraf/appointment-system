package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentConflictException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentDateInPastException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidAppointmentDateRangeException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import com.pedrovieira.appointmentsystem.repository.specification.AppointmentSpecification;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Appointments service.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final Clock clock;

  /**
   * Create Appointment.
   */
  public Appointment create(
      User user,
      AppointmentRequest request
  ) {
    if (request.appointmentDate().isBefore(LocalDateTime.now(clock))) {
      throw new BusinessRuleException("Appointment date is before current date");
    }

    if (appointmentRepository.existsByUserAndAppointmentDate(
        user,
        request.appointmentDate()
    )) {
      throw new BusinessRuleException(
          "User already has an appointment at this time"
      );
    }

    return appointmentRepository.save(request.toEntity(user));
  }

  /**
   * Get Appointments by ID and User.
   */
  public Appointment getById(User user, Long id) {
    Optional<Appointment> appointment = appointmentRepository.findByUserAndId(user, id);

    if (appointment.isEmpty()) {
      throw new AppointmentNotFoundException();
    }

    return appointment.get();
  }

  /**
   * Update Appointment.
   */
  @Transactional
  public Appointment update(
      User user,
      Long id,
      UpdateAppointmentRequest request
  ) {
    Appointment appointment = getById(user, id);

    if (request.appointmentDate() != null
        && request.appointmentDate().isBefore(LocalDateTime.now())) {

      throw new AppointmentDateInPastException(
          "Appointment date must be in the future."
      );
    }

    if (request.appointmentDate() != null
        && appointmentRepository.existsByUserAndAppointmentDateAndIdNot(
        user,
        request.appointmentDate(),
        id
    )) {

      throw new AppointmentConflictException(
          "Appointment date conflicts with another appointment."
      );
    }

    final List<Integer> previousOffsets = appointment.getReminderOffsetsMinutes();
    if (request.reminderOffsetsMinutes() != null) {
      appointment.replaceReminderOffsets(request.reminderOffsetsMinutes());
    }

    if (request.title() != null) {
      appointment.setTitle(request.title());
    }

    if (request.description() != null) {
      appointment.setDescription(request.description());
    }

    boolean appointmentDateChanged = request.appointmentDate() != null
        && !request.appointmentDate().equals(appointment.getAppointmentDate());
    if (appointmentDateChanged) {
      appointment.setAppointmentDate(request.appointmentDate());
    }

    if (appointmentDateChanged || request.reminderOffsetsMinutes() != null) {
      appointment.refreshReminderSchedule(
          LocalDateTime.now(clock), appointmentDateChanged, previousOffsets);
    }

    return appointmentRepository.save(appointment);
  }

  /**
   * Delete Appointment.
   */
  public void delete(User user, Long id) {
    Appointment appointment = getById(user, id);

    appointmentRepository.delete(appointment);
  }

  /**
   * Get all appointments for a user with optional filters and pagination.
   */
  public Page<Appointment> getAll(
      User user,
      AppointmentSearchCriteria criteria,
      Pageable pageable
  ) {
    if (criteria.startDate() != null
        && criteria.endDate() != null
        && criteria.endDate().isBefore(criteria.startDate())) {
      throw new InvalidAppointmentDateRangeException();
    }

    Specification<Appointment> specification =
        AppointmentSpecification.withFilters(user, criteria);

    return appointmentRepository.findAll(
        specification,
        pageable
    );
  }

  /**
   * Complete an appointment.
   */
  @Transactional
  public Appointment complete(User user, Long appointmentId) {
    Appointment appointment =
        getById(user, appointmentId);

    appointment.markAsCompleted();

    return appointmentRepository.save(appointment);
  }

  /**
   * Confirm an appointment.
   */
  @Transactional
  public Appointment confirm(User user, Long appointmentId) {
    Appointment appointment =
        getById(user, appointmentId);

    appointment.markAsConfirmed();
    appointment.refreshReminderSchedule(LocalDateTime.now(clock), false, List.of());

    return appointmentRepository.save(appointment);
  }

  /**
   * Cancel an appointment.
   */
  @Transactional
  public Appointment cancel(User user, Long appointmentId) {
    Appointment appointment =
        getById(user, appointmentId);

    appointment.markAsCanceled();

    return appointmentRepository.save(appointment);
  }
}
