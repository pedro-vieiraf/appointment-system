package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import java.time.LocalDateTime;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

/**
 * Appointment repository.
 */
@Repository
public interface AppointmentRepository
    extends JpaRepository<Appointment, Long>,
    JpaSpecificationExecutor<Appointment> {

  /**
   * Verify if time is taken to new appointments.
   */
  boolean existsByUserAndAppointmentDate(User attr0, LocalDateTime attr1);

  /**
   * Get by ID by user.
   */
  Optional<Appointment> findByIdAndUser(User user, Long id);

  /**
   * Verify if another appointment exists at the given time.
   */
  boolean existsByUserAndAppointmentDateAndIdNot(
      User user,
      LocalDateTime appointmentDate,
      Long id
  );
}
