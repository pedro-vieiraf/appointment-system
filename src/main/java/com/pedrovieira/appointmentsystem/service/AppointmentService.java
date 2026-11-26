package com.pedrovieira.appointmentsystem.service;

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
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.repository.specification.AppointmentSpecification;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
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
  private final UserRepository userRepository;
  private final Clock clock;

  /**
   * Create Appointment.
   */
  @Transactional
  public Appointment create(
      User user,
      AppointmentRequest request
  ) {
    if (!request.appointmentDate().isAfter(LocalDateTime.now(clock))) {
      throw new BusinessRuleException("Appointment date is before current date");
    }

    lockSchedule(user);
    expireHolds(user);
    if (appointmentRepository.existsByUserAndAppointmentDateAndStatusNot(
        user,
        request.appointmentDate(),
        AppointmentStatus.CANCELLED
    )) {
      throw new BusinessRuleException(
          "Você já tem um agendamento nesse horário. Escolha outro horário."
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
    lockSchedule(user);
    final LocalDateTime now = LocalDateTime.now(clock);
    appointment.requireUnexpiredHold(now);

    if (request.appointmentDate() != null
        && !request.appointmentDate().isAfter(now)) {

      throw new AppointmentDateInPastException(
          "Appointment date must be in the future."
      );
    }

    expireHolds(user);
    if (request.appointmentDate() != null
        && appointment.getStatus() != AppointmentStatus.CANCELLED
        && appointmentRepository.existsByUserAndAppointmentDateAndIdNotAndStatusNot(
        user,
        request.appointmentDate(),
        id,
        AppointmentStatus.CANCELLED
    )) {

      throw new AppointmentConflictException(
          "Esse horário coincide com outro agendamento. Escolha outro horário."
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

    if (request.durationMinutes() != null) {
      appointment.changeDuration(request.durationMinutes());
    }

    boolean appointmentDateChanged = request.appointmentDate() != null
        && !request.appointmentDate().equals(appointment.getAppointmentDate());
    if (appointmentDateChanged) {
      appointment.setAppointmentDate(request.appointmentDate());
    }

    if (appointmentDateChanged || request.reminderOffsetsMinutes() != null) {
      appointment.refreshReminderSchedule(
          now, appointmentDateChanged, previousOffsets);
    }

    return appointmentRepository.save(appointment);
  }

  /**
   * Delete Appointment.
   */
  @Transactional
  public void delete(User user, Long id) {
    Appointment appointment = getById(user, id);

    lockSchedule(user);
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

    lockSchedule(user);
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

    lockSchedule(user);
    LocalDateTime now = LocalDateTime.now(clock);
    appointment.requireUnexpiredHold(now);
    appointment.markAsConfirmed();
    appointment.refreshReminderSchedule(now, false, List.of());

    return appointmentRepository.save(appointment);
  }

  /**
   * Cancel an appointment.
   */
  @Transactional
  public Appointment cancel(User user, Long appointmentId) {
    Appointment appointment =
        getById(user, appointmentId);

    lockSchedule(user);
    appointment.markAsCanceled();

    return appointmentRepository.save(appointment);
  }

  private void expireHolds(User user) {
    Long ownerId = user.getId();
    if (ownerId != null) {
      appointmentRepository.expirePendingHolds(
          ownerId, LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));
    }
  }

  private void lockSchedule(User user) {
    userRepository.findForSchedulingUpdateById(user.getId());
  }
}
