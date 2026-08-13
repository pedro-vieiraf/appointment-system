package com.pedrovieira.appointmentsystem.dto.request;

import java.time.LocalDateTime;

public record AppointmentSearchCriteria(
    String title,
    LocalDateTime startDate,
    LocalDateTime endDate
) {

}
