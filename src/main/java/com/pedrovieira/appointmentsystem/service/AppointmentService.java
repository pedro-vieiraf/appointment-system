package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentConflictException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentDateInPastException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

/**
 * Appointments service.
 */
@Service
@RequiredArgsConstructor
public class AppointmentService {

  private final AppointmentRepository appointmentRepository;
  private final Clock clock;

  /**
   * CreateAppointment Function.
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

    Appointment appointment =
        new Appointment(
            user,
            request.title(),
            request.description(),
            request.appointmentDate()
        );

    return appointmentRepository.save(appointment);
  }

  /**
   * GetAll Appointments by User.
   */
  public List<Appointment> getAll(User user) {
    return appointmentRepository.findAllByUser(user);
  }

  /**
   * Get Appointments by ID and User.
   */
  public Appointment getById(User user, Long id) {
    Optional<Appointment> appointment = appointmentRepository.findByIdAndUser(user, id);

    if (appointment.isEmpty()) {
      throw new AppointmentNotFoundException();
    }

    return appointment.get();
  }

  /**
   * Update Appointment.
   */
  public Appointment update(
      User user,
      Long id,
      UpdateAppointmentRequest request
  ) {
    Optional<Appointment> foundAppointment =
        appointmentRepository.findByIdAndUser(user, id);

    if (foundAppointment.isEmpty()) {
      throw new AppointmentNotFoundException();
    }

    Appointment appointment = foundAppointment.get();

    if (request.appointmentDate() != null
        && request.appointmentDate().isBefore(LocalDateTime.now())) {

      throw new AppointmentDateInPastException(
          "Appointment date must be in the future."
      );
    }

    if (request.appointmentDate() != null
        && appointmentRepository.existsByUserAndAppointmentDate(
        user,
        request.appointmentDate()
    )) {

      throw new AppointmentConflictException(
          "Appointment date conflicts with another appointment."
      );
    }

    if (request.title() != null) {
      appointment.setTitle(request.title());
    }

    if (request.description() != null) {
      appointment.setDescription(request.description());
    }

    if (request.appointmentDate() != null) {
      appointment.setAppointmentDate(request.appointmentDate());
    }

    return appointmentRepository.save(appointment);
  }
}
