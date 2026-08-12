package com.pedrovieira.appointmentsystem.exception.users;

public class InvalidPasswordException extends RuntimeException {

  public InvalidPasswordException() {
    super("Current password is incorrect.");
  }
}
