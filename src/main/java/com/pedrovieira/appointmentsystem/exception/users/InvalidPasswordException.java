package com.pedrovieira.appointmentsystem.exception.users;

/**
 * Exception for invalid password.
 */
public class InvalidPasswordException extends RuntimeException {

  /**
   * Error message for InvalidPasswordException.
   */
  public InvalidPasswordException() {
    super("Current password is incorrect.");
  }
}
