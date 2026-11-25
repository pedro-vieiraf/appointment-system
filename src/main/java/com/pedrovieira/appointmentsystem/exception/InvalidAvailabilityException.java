package com.pedrovieira.appointmentsystem.exception;

/**
 * Raised when working hours cannot form a valid weekly schedule.
 */
public class InvalidAvailabilityException extends RuntimeException {

  /** Creates an exception with the validation failure. */
  public InvalidAvailabilityException(String message) {
    super(message);
  }
}
