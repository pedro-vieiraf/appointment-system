package com.pedrovieira.appointmentsystem.dto.response;

import java.time.Instant;

/**
 * A future time interval within a professional's configured working hours.
 */
public record CandidateSlot(Instant start, Instant end) {
}
