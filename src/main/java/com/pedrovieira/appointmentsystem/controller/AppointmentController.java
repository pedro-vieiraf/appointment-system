package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.response.AppointmentResponse;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * Controller that will handle appointments.
 */
@RestController
@RequiredArgsConstructor
@RequestMapping(value = "/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;

  /**
   * POST Route that will create a new appointment.
   */
  @PostMapping
  public ResponseEntity<AppointmentResponse> create(
      Authentication authentication,
      @Valid
      @RequestBody AppointmentRequest request
  ) {
    User user = (User) authentication.getPrincipal();
    Appointment appointment = appointmentService.create(user, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(AppointmentResponse.fromAppointment(appointment));
  }
}
