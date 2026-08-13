package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.response.AppointmentResponse;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import com.pedrovieira.appointmentsystem.validation.OrderedValidation;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import java.util.List;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.Authentication;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
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
      @Validated(OrderedValidation.class)
      @RequestBody AppointmentRequest request
  ) {
    User user = (User) authentication.getPrincipal();
    Appointment appointment = appointmentService.create(user, request);

    return ResponseEntity
        .status(HttpStatus.CREATED)
        .body(AppointmentResponse.fromAppointment(appointment));
  }

  /**
   * GET Route that will get the list of appointment with filtering function.
   */
  @GetMapping
  public ResponseEntity<List<AppointmentResponse>> getAll(
      Authentication authentication,
      @RequestParam(required = false) String title,
      @RequestParam(required = false) LocalDateTime startDate,
      @RequestParam(required = false) LocalDateTime endDate
  ) {
    User user = (User) authentication.getPrincipal();

    AppointmentSearchCriteria criteria =
        new AppointmentSearchCriteria(
            title,
            startDate,
            endDate
        );

    List<Appointment> appointments =
        appointmentService.search(user, criteria);

    return ResponseEntity.ok(
        appointments.stream()
            .map(AppointmentResponse::fromAppointment)
            .toList()
    );
  }

  /**
   * GET by ID Route that will get the specific appointment.
   */
  @GetMapping("/{id}")
  public ResponseEntity<AppointmentResponse> getById(
      Authentication authentication,
      @PathVariable long id
  ) {
    User user = (User) authentication.getPrincipal();

    Appointment appointment = appointmentService.getById(user, id);

    return ResponseEntity.ok(
        AppointmentResponse.fromAppointment(appointment)
    );
  }

  /**
   * PATCH Route to update appointments.
   */
  @PatchMapping("/{id}")
  public ResponseEntity<AppointmentResponse> update(
      Authentication authentication,
      @PathVariable long id,
      @Valid @RequestBody UpdateAppointmentRequest request
  ) {
    User user = (User) authentication.getPrincipal();

    Appointment appointment =
        appointmentService.update(user, id, request);

    return ResponseEntity.ok(
        AppointmentResponse.fromAppointment(appointment)
    );
  }

  /**
   * DELETE Route to delete appointments.
   */
  @DeleteMapping("/{id}")
  public ResponseEntity<Void> delete(
      Authentication authentication,
      @PathVariable long id
  ) {
    User user = (User) authentication.getPrincipal();

    appointmentService.delete(user, id);

    return ResponseEntity.noContent().build();
  }
}
