package com.pedrovieira.appointmentsystem.exception.appointments;

/**
 * Indicates that reminder offsets are missing a value, non-positive, or duplicated.
 */
public class InvalidReminderOffsetsException extends RuntimeException {

  /**
   * Creates an exception with the reminder validation rule.
   */
  public InvalidReminderOffsetsException() {
    super("Reminder offsets must be unique positive numbers of minutes.");
  }
}
