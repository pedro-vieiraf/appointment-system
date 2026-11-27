package com.pedrovieira.appointmentsystem.exception;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/** A public booking failure with a message suitable for the customer. */
@Getter
public class PublicBookingException extends RuntimeException {

  private final HttpStatus status;
  private final Long retryAfterSeconds;

  /** Describes why a public booking operation failed. */
  public PublicBookingException(HttpStatus status, String message) {
    this(status, message, null);
  }

  /** Describes a temporary failure and its retry delay. */
  public PublicBookingException(HttpStatus status, String message, Long retryAfterSeconds) {
    super(message);
    this.status = status;
    this.retryAfterSeconds = retryAfterSeconds;
  }

  /** Reports that the professional is no longer sharing this agenda link. */
  public static PublicBookingException unsharedAgenda() {
    return new PublicBookingException(HttpStatus.NOT_FOUND,
        "Esta agenda não está mais disponível. Peça um novo link ao profissional.");
  }
}
