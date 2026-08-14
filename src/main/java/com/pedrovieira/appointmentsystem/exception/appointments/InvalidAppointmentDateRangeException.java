package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * Exception for invalid Appointment Date Range.
 */
public class InvalidAppointmentDateRangeException extends RuntimeException {

  /**
   * Return error message.
   */
  public InvalidAppointmentDateRangeException() {
    super("End date must be after start date.");
  }
}
