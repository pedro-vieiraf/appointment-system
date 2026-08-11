package com.pedrovieira.appointmentsystem.exception;

/**
 * Exception for BusinessRuleException.
 */
public class BusinessRuleException extends RuntimeException {

  /**
   * Set the error response.
   */
  public BusinessRuleException(String message) {
    super(message);
  }
}
