package com.pedrovieira.appointmentsystem.exception.user;

public class InvalidCredentialsException extends RuntimeException {

  public InvalidCredentialsException(String message) {
    super(message);
  }
}
