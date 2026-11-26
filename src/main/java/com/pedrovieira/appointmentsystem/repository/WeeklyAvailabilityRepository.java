package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.WeeklyAvailability;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Stores one weekly availability schedule per professional.
 */
public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, Long> {

  /** Finds the schedule for an active public link. */
  Optional<WeeklyAvailability> findByPublicLinkId(UUID publicLinkId);
}
