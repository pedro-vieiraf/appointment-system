package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import com.pedrovieira.appointmentsystem.service.WeeklyAvailabilityService;
import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Public read-only access to an explicitly shared agenda.
 */
@RestController
@RequiredArgsConstructor
public class PublicAvailabilityController {

  private final WeeklyAvailabilityService availabilityService;

  /** Lists available slots on a date in the professional's time zone. */
  @GetMapping("/public/availability/{linkId}/slots")
  public ResponseEntity<List<CandidateSlot>> slots(
      @PathVariable UUID linkId,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    return availabilityService.findPublicFreeSlots(linkId, date)
        .map(slots -> ResponseEntity.ok().cacheControl(CacheControl.noStore()).body(slots))
        .orElseThrow(PublicBookingException::unsharedAgenda);
  }
}
