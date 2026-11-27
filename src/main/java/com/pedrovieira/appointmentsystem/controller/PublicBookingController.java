package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.PublicBookingRequest;
import com.pedrovieira.appointmentsystem.service.PublicBookingService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import java.time.Instant;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.CacheControl;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/** Accepts public booking requests. */
@RestController
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.requests.enabled", havingValue = "true")
@RequestMapping("/public/availability/{linkId}/requests")
public class PublicBookingController {

  private final PublicBookingService bookingService;

  /** Creates a pending request for an available slot. */
  @PostMapping
  public ResponseEntity<BookingHold> request(@PathVariable UUID linkId,
      @Valid @RequestBody PublicBookingRequest request, HttpServletRequest httpRequest) {
    bookingService.checkRateLimit(linkId, httpRequest.getRemoteAddr());
    Instant expiresAt = bookingService.request(linkId, request);
    return ResponseEntity.status(HttpStatus.CREATED).cacheControl(CacheControl.noStore())
        .body(new BookingHold(expiresAt));
  }

  /** The deadline for reviewing the pending request. */
  public record BookingHold(Instant expiresAt) {
  }
}
