package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.WeeklyAvailability;
import java.util.Optional;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/**
 * Stores one weekly availability schedule per professional.
 */
public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, Long> {

  /** Finds the schedule for an active public link. */
  Optional<WeeklyAvailability> findByPublicLinkId(UUID publicLinkId);

  /** Resolves the owner without loading the schedule before acquiring its lock. */
  @Query("select a.userId from WeeklyAvailability a where a.publicLinkId = :linkId")
  Optional<Long> findPublicOwnerId(UUID linkId);
}
