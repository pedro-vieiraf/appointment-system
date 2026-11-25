package com.pedrovieira.appointmentsystem.dto.request;

import com.pedrovieira.appointmentsystem.entity.AvailabilityWindow;
import jakarta.validation.Valid;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import java.util.List;

/**
 * Settings for a professional's weekly working hours and appointment duration.
 */
public record WeeklyAvailabilityRequest(
    @NotBlank String timeZone,
    @Positive @Max(1440) int durationMinutes,
    @NotNull List<@NotNull @Valid AvailabilityWindow> windows
) {
}
