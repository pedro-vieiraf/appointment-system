package com.pedrovieira.appointmentsystem.exception.users;

/**
 * Exception for EmailNotFound.
 */
public class EmailNotFoundException extends RuntimeException {

  /**
   * Exception that sets the standard message.
   */
  public EmailNotFoundException() {
    super("Email not found.");
  }
}
