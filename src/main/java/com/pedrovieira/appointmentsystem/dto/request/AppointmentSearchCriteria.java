package com.pedrovieira.appointmentsystem.dto.request;

import java.time.LocalDateTime;

/**
 * Appointment getAll criteria DTO.
 */
public record AppointmentSearchCriteria(
    String title,
    LocalDateTime startDate,
    LocalDateTime endDate
) {

}
