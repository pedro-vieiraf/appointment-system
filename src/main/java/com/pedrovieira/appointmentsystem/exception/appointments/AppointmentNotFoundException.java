package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * Appointment not found exception.
 */
public class AppointmentNotFoundException extends RuntimeException {

  /**
   * Message for appointment not found.
   */
  public AppointmentNotFoundException() {
    super("Appointment not found.");
  }
}
