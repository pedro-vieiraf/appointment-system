package com.pedrovieira.appointmentsystem.dto.request;

import java.time.LocalDateTime;

/**
 * Appointment search criteria DTO.
 */
public record AppointmentSearchCriteria(
    String title,
    LocalDateTime startDate,
    LocalDateTime endDate
) {

}
