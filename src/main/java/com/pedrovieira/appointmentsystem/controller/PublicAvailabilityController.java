package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import com.pedrovieira.appointmentsystem.service.WeeklyAvailabilityService;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.List;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.core.io.ClassPathResource;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.CacheControl;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.util.HtmlUtils;

/**
 * Public read-only access to an explicitly shared agenda.
 */
@RestController
@RequiredArgsConstructor
public class PublicAvailabilityController {

  private final WeeklyAvailabilityService availabilityService;

  @Value("${booking.requests.enabled:false}")
  private boolean bookingEnabled;

  /** Shows the public booking page for an active share link. */
  @GetMapping(value = "/public/availability/{linkId}", produces = MediaType.TEXT_HTML_VALUE)
  public ResponseEntity<String> bookingPage(@PathVariable UUID linkId) throws IOException {
    String timeZone = availabilityService.findPublicTimeZone(linkId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
    var rules = ZoneId.of(timeZone).getRules();
    String fixedOffset = rules.isFixedOffset()
        ? Integer.toString(rules.getOffset(Instant.EPOCH).getTotalSeconds()) : "";
    String html = new ClassPathResource("booking-page.html")
        .getContentAsString(StandardCharsets.UTF_8)
        .replace("{{LINK_ID}}", linkId.toString())
        .replace("{{TIME_ZONE}}", HtmlUtils.htmlEscape(timeZone))
        .replace("{{BOOKING_ENABLED}}", Boolean.toString(bookingEnabled))
        .replace("{{FIXED_OFFSET}}", fixedOffset);
    return ResponseEntity.ok().cacheControl(CacheControl.noStore())
        .contentType(new MediaType("text", "html", StandardCharsets.UTF_8)).body(html);
  }

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
