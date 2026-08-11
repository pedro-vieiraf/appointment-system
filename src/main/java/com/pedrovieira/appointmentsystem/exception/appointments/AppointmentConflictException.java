package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * AppointmentConflictException.
 */
public class AppointmentConflictException extends RuntimeException {

  /**
   * Method of exception that will define the error message
   */
  public AppointmentConflictException(String message) {
    super(message);
  }
}
