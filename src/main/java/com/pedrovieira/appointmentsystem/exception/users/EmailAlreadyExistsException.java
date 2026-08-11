package com.pedrovieira.appointmentsystem.exception.users;

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
