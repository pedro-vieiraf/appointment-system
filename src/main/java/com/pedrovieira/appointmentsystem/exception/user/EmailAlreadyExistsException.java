package com.pedrovieira.appointmentsystem.exception.user;

/**
 * Exception for EmailAlreadyExists.
 */
public class EmailAlreadyExistsException extends RuntimeException {

  /**
   * Exception that sets the standard message.
   */
  public EmailAlreadyExistsException() {
    super("Email already registered.");
  }
}
