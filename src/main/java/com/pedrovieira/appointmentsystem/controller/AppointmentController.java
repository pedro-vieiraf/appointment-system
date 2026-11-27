package com.pedrovieira.appointmentsystem.controller;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.response.AppointmentResponse;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.ErrorResponse;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import com.pedrovieira.appointmentsystem.validation.OrderedValidation;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import jakarta.validation.Valid;
import java.time.LocalDateTime;
import lombok.RequiredArgsConstructor;
import org.springdoc.core.annotations.ParameterObject;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.web.PageableDefault;
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
@SecurityRequirement(name = "bearerAuth")
@RequiredArgsConstructor
@RequestMapping(value = "/appointments")
public class AppointmentController {

  private final AppointmentService appointmentService;

  /**
   * POST Route that will create a new appointment.
   */
  @PostMapping
  @ApiResponse(responseCode = "201", description = "Appointment created",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid or malformed request body",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "409",
      description = "Appointment date is in the past or overlaps another appointment",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
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
  @ApiResponse(responseCode = "200", description = "Page of matching appointments",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400", description = "Invalid date range",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @Operation(summary = "List the authenticated user's appointments")
  public ResponseEntity<Page<AppointmentResponse>> getAll(
      Authentication authentication,
      @Parameter(description = "Case-insensitive title substring")
      @RequestParam(required = false) String title,
      @Parameter(description = "Inclusive start date and time, without a time zone")
      @RequestParam(required = false) LocalDateTime startDate,
      @Parameter(description = "Inclusive end date and time, without a time zone")
      @RequestParam(required = false) LocalDateTime endDate,
      @ParameterObject
      @PageableDefault(
          size = 10,
          sort = "appointmentDate",
          direction = Sort.Direction.ASC
      )
      Pageable pageable
  ) {
    User user = (User) authentication.getPrincipal();

    AppointmentSearchCriteria criteria =
        new AppointmentSearchCriteria(
            title,
            startDate,
            endDate
        );

    Page<Appointment> appointments =
        appointmentService.getAll(
            user,
            criteria,
            pageable
        );

    Page<AppointmentResponse> response =
        appointments.map(AppointmentResponse::fromAppointment);

    return ResponseEntity.ok(response);
  }

  /**
   * GET by ID Route that will get the specific appointment.
   */
  @GetMapping("/{id}")
  @ApiResponse(responseCode = "200", description = "Appointment found",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
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
  @ApiResponse(responseCode = "200", description = "Appointment updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "400",
      description = "Invalid or malformed request body, or appointment date is in the past",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409",
      description = "The interval is occupied or the appointment changed concurrently",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
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
  @ApiResponse(responseCode = "204", description = "Appointment deleted",
      content = @Content)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409", description = "Appointment changed concurrently",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<Void> delete(
      Authentication authentication,
      @PathVariable long id
  ) {
    User user = (User) authentication.getPrincipal();

    appointmentService.delete(user, id);

    return ResponseEntity.noContent().build();
  }

  /**
   * PATCH Route to confirm an appointment.
   */
  @PatchMapping("/{id}/confirm")
  @Operation(description = "Changes PENDING to CONFIRMED.")
  @ApiResponse(responseCode = "200", description = "Appointment status updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409",
      description = "Appointment is not pending or changed concurrently",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<AppointmentResponse> confirm(
      Authentication authentication,
      @PathVariable Long id
  ) {
    User user = (User) authentication.getPrincipal();

    Appointment appointment =
        appointmentService.confirm(user, id);

    return ResponseEntity.ok(
        AppointmentResponse.fromAppointment(appointment)
    );
  }

  /**
   * PATCH Route to cancel an appointment.
   */
  @PatchMapping("/{id}/cancel")
  @Operation(description = "Changes PENDING or CONFIRMED to CANCELLED.")
  @ApiResponse(responseCode = "200", description = "Appointment status updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409",
      description = "Appointment cannot be cancelled or changed concurrently",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<AppointmentResponse> cancel(
      Authentication authentication,
      @PathVariable Long id
  ) {
    User user = (User) authentication.getPrincipal();

    Appointment appointment =
        appointmentService.cancel(user, id);

    return ResponseEntity.ok(
        AppointmentResponse.fromAppointment(appointment)
    );
  }

  /**
   * PATCH Route to confirm an appointment.
   */
  @PatchMapping("/{id}/complete")
  @Operation(description = "Changes CONFIRMED to COMPLETED.")
  @ApiResponse(responseCode = "200", description = "Appointment status updated",
      useReturnTypeSchema = true)
  @ApiResponse(responseCode = "403", description = "Authentication is required",
      content = @Content)
  @ApiResponse(responseCode = "404",
      description = "Appointment does not exist for the authenticated user",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  @ApiResponse(responseCode = "409",
      description = "Appointment is not confirmed or changed concurrently",
      content = @Content(mediaType = "application/json",
      schema = @Schema(implementation = ErrorResponse.class)))
  public ResponseEntity<AppointmentResponse> complete(
      Authentication authentication,
      @PathVariable Long id
  ) {
    User user = (User) authentication.getPrincipal();

    Appointment appointment =
        appointmentService.complete(user, id);

    return ResponseEntity.ok(
        AppointmentResponse.fromAppointment(appointment)
    );
  }
}
