package com.pedrovieira.appointmentsystem.exception.users;

/**
 * Invalid Credentials Exception.
 */
public class InvalidCredentialsException extends RuntimeException {

  /**
   * Error Message for exception.
   */
  public InvalidCredentialsException(String message) {
    super(message);
  }
}
