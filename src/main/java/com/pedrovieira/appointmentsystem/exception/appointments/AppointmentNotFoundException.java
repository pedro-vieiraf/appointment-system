package com.pedrovieira.appointmentsystem.exception.appointments;

public class AppointmentNotFoundException extends RuntimeException {

  public AppointmentNotFoundException(String message) {
    super(message);
  }
}
