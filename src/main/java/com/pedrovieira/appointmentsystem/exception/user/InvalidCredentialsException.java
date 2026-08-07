package com.pedrovieira.appointmentsystem.exception.user;

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
