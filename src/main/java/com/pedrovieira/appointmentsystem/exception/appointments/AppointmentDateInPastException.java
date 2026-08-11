package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * Exception for AppointmentDate in Past
 */
public class AppointmentDateInPastException extends RuntimeException {

  /**
   * Method to define error message.
   */
  public AppointmentDateInPastException(String message) {
    super(message);
  }
}
