package com.pedrovieira.appointmentsystem.exception;

import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentConflictException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentDateInPastException;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidAppointmentDateRangeException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidReminderOffsetsException;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.exception.users.EmailNotFoundException;
import com.pedrovieira.appointmentsystem.exception.users.InvalidCredentialsException;
import com.pedrovieira.appointmentsystem.exception.users.InvalidPasswordException;
import jakarta.servlet.http.HttpServletRequest;
import java.time.LocalDateTime;
import org.springframework.http.HttpStatus;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.ResponseStatus;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Global Exception Handler to catch all exceptions in Controllers.
 */
@RestControllerAdvice
public class GlobalExceptionHandler {

  /**
   * ExceptionHandler for EmailAlreadyExists.
   */
  @ExceptionHandler(EmailAlreadyExistsException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleEmailAlreadyExists(
      EmailAlreadyExistsException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.CONFLICT,
        exception.getMessage(),
        request
    );
  }

  /**
   * ExceptionHandler for EmailNotFound.
   */
  @ExceptionHandler(EmailNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleEmailNotFound(
      EmailNotFoundException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.NOT_FOUND,
        exception.getMessage(),
        request
    );
  }

  /**
   * ExceptionHandler for Invalid Credentials.
   */
  @ExceptionHandler(InvalidCredentialsException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleInvalidCredentials(
      InvalidCredentialsException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.UNAUTHORIZED,
        exception.getMessage(),
        request
    );
  }

  /**
   * ExceptionHandler for MethodArgumentNotValid.
   */
  @ExceptionHandler(MethodArgumentNotValidException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleValidation(
      MethodArgumentNotValidException exception,
      HttpServletRequest request
  ) {
    String message = exception
        .getBindingResult()
        .getFieldError()
        .getDefaultMessage();

    return createErrorResponse(
        HttpStatus.BAD_REQUEST,
        message,
        request
    );
  }

  /**
   * ExceptionHandler for HttpMessageNotReadable.
   */
  @ExceptionHandler(HttpMessageNotReadableException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleMalformedJson(
      HttpMessageNotReadableException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.BAD_REQUEST,
        "Malformed JSON request.",
        request
    );
  }

  /**
   * Exception handler for BusinessRuleException.
   */
  @ExceptionHandler(BusinessRuleException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleBusinessRuleException(
      BusinessRuleException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.CONFLICT,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for AppointmentNotFound.
   */
  @ExceptionHandler(AppointmentNotFoundException.class)
  @ResponseStatus(HttpStatus.NOT_FOUND)
  public ErrorResponse handleAppointmentNotFound(
      AppointmentNotFoundException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.NOT_FOUND,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for AppointmentConflict.
   */
  @ExceptionHandler(AppointmentConflictException.class)
  @ResponseStatus(HttpStatus.CONFLICT)
  public ErrorResponse handleAppointmentConflict(
      AppointmentConflictException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.CONFLICT,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for AppointmentDateInPast.
   */
  @ExceptionHandler(AppointmentDateInPastException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleAppointmentDateInPast(
      AppointmentDateInPastException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.BAD_REQUEST,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for InvalidPassword.
   */
  @ExceptionHandler(InvalidPasswordException.class)
  @ResponseStatus(HttpStatus.UNAUTHORIZED)
  public ErrorResponse handleInvalidPassword(
      InvalidPasswordException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.UNAUTHORIZED,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for InvalidAppointmentDateRange.
   */
  @ExceptionHandler(InvalidAppointmentDateRangeException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleInvalidAppointmentDateRange(
      InvalidAppointmentDateRangeException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(
        HttpStatus.BAD_REQUEST,
        exception.getMessage(),
        request
    );
  }

  /**
   * Exception handler for invalid reminder settings.
   */
  @ExceptionHandler(InvalidReminderOffsetsException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleInvalidReminderOffsets(
      InvalidReminderOffsetsException exception,
      HttpServletRequest request
  ) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  /**
   * Maps invalid weekly working hours to a bad request.
   */
  @ExceptionHandler(InvalidAvailabilityException.class)
  @ResponseStatus(HttpStatus.BAD_REQUEST)
  public ErrorResponse handleInvalidAvailability(
      InvalidAvailabilityException exception, HttpServletRequest request
  ) {
    return createErrorResponse(HttpStatus.BAD_REQUEST, exception.getMessage(), request);
  }

  private ErrorResponse createErrorResponse(
      HttpStatus status,
      String message,
      HttpServletRequest request
  ) {
    return new ErrorResponse(
        LocalDateTime.now(),
        status.value(),
        status.getReasonPhrase(),
        message,
        request.getRequestURI()
    );
  }
}