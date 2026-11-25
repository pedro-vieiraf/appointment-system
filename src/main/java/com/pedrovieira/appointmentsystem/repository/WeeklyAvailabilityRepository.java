package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.WeeklyAvailability;
import org.springframework.data.jpa.repository.JpaRepository;

/**
 * Stores one weekly availability schedule per professional.
 */
public interface WeeklyAvailabilityRepository extends JpaRepository<WeeklyAvailability, Long> {
}
