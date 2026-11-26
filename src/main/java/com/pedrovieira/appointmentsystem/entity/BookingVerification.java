package com.pedrovieira.appointmentsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import java.time.Instant;
import java.time.LocalDate;
import java.util.UUID;
import lombok.Getter;

/** A booking request awaiting verification. It does not occupy an appointment slot. */
@Entity
@Getter
public class BookingVerification {

  @Id
  private UUID token;

  @Column(nullable = false)
  private UUID linkId;

  @Column(nullable = false)
  private String guestName;

  @Column(nullable = false)
  private String guestEmail;

  @Column(nullable = false)
  private LocalDate requestedDate;

  @Column(nullable = false)
  private Instant slotStart;

  @Column(nullable = false)
  private Instant slotEnd;

  @Column(nullable = false)
  private Instant expiresAt;

  /** Constructs an instance for JPA. */
  protected BookingVerification() {
  }

  /** Captures the requested slot and a one-time verification token. */
  public BookingVerification(UUID linkId, String guestName, String guestEmail,
      LocalDate requestedDate, Instant slotStart, Instant slotEnd, Instant expiresAt) {
    token = UUID.randomUUID();
    this.linkId = linkId;
    this.guestName = guestName;
    this.guestEmail = guestEmail;
    this.requestedDate = requestedDate;
    this.slotStart = slotStart;
    this.slotEnd = slotEnd;
    this.expiresAt = expiresAt;
  }
}
