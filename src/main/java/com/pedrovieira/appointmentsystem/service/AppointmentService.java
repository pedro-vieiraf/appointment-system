package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
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

  public List<Appointment> findAll() {
    return null;
  }
}
