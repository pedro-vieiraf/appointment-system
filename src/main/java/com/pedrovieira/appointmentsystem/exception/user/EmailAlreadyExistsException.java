package com.pedrovieira.appointmentsystem.exception.user;

public class EmailAlreadyExistsException extends RuntimeException {

  public EmailAlreadyExistsException() {
    super("Email already registered.");
  }
}
