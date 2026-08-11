package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * Exception for AppointmentDate in the past.
 */
public class AppointmentDateInPastException extends RuntimeException {

  /**
   * Method to define error message.
   */
  public AppointmentDateInPastException(String message) {
    super(message);
  }
}
