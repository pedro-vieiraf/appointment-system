package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;
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
  boolean existsByUserAndAppointmentDateAndStatusNot(
      User user,
      LocalDateTime appointmentDate,
      AppointmentStatus excludedStatus
  );

  /**
   * Get by ID by user.
   */
  Optional<Appointment> findByUserAndId(User user, Long id);

  /**
   * Verify if another appointment exists at the given time.
   */
  boolean existsByUserAndAppointmentDateAndIdNotAndStatusNot(
      User user,
      LocalDateTime appointmentDate,
      Long id,
      AppointmentStatus excludedStatus
  );

  /**
   * Finds non-cancelled appointments whose start could affect the requested slot range.
   */
  @Query("""
      select a from Appointment a
      where a.user.id = :ownerId and a.status <> :excludedStatus
        and a.appointmentDate >= :start and a.appointmentDate < :end
      """)
  List<Appointment> findSlotOccupants(
      long ownerId, LocalDateTime start, LocalDateTime end, AppointmentStatus excludedStatus);
}
