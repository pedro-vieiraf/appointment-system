package com.pedrovieira.appointmentsystem.exception;

import java.time.LocalDateTime;

/**
 * Standard response format for errors.
 */
public record ErrorResponse(
    LocalDateTime timestamp,
    int status,
    String error,
    String message,
    String path
) {

}