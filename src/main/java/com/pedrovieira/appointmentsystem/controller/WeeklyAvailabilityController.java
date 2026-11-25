package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.WeeklyAvailabilityRequest;
import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.service.WeeklyAvailabilityService;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDate;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

/**
 * Authenticated endpoints for configuring and previewing weekly working hours.
 */
@RestController
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping("/users/me/availability")
public class WeeklyAvailabilityController {

  private final WeeklyAvailabilityService availabilityService;

  /** Replaces all weekly availability settings for the current account. */
  @PutMapping
  public WeeklyAvailabilityRequest replace(
      @AuthenticationPrincipal User user, @Valid @RequestBody WeeklyAvailabilityRequest request
  ) {
    return availabilityService.replace(user.getId(), request);
  }

  /** Reads the current account schedule. */
  @GetMapping
  public ResponseEntity<WeeklyAvailabilityRequest> get(@AuthenticationPrincipal User user) {
    return ResponseEntity.of(availabilityService.get(user.getId()));
  }

  /** Previews candidate intervals on one local calendar date. */
  @GetMapping("/candidate-slots")
  public List<CandidateSlot> preview(
      @AuthenticationPrincipal User user,
      @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate date
  ) {
    return availabilityService.preview(user.getId(), date);
  }
}
