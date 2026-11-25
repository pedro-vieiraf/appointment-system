package com.pedrovieira.appointmentsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.validation.constraints.NotNull;
import java.time.DayOfWeek;
import java.time.LocalTime;

/**
 * A recurring working-hours window on one weekday.
 */
@Embeddable
public record AvailabilityWindow(
    @NotNull @Enumerated(EnumType.STRING) @Column(nullable = false) DayOfWeek dayOfWeek,
    @NotNull @Column(name = "starts_at", nullable = false) LocalTime start,
    @NotNull @Column(name = "ends_at", nullable = false) LocalTime end
) {
}
