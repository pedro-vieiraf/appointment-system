package com.pedrovieira.appointmentsystem.entity;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import java.time.Instant;
import java.time.LocalDateTime;
import lombok.Getter;

/** An email to deliver after the professional decides a verified booking request. */
@Entity
@Table(name = "booking_decision_notifications",
    indexes = @Index(name = "ix_booking_decision_due", columnList = "next_attempt_at,id"))
@Getter
public class BookingDecisionNotification {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Column(nullable = false)
  private String recipient;

  @Enumerated(EnumType.STRING)
  @Column(nullable = false)
  private AppointmentStatus decision;

  @Column(nullable = false)
  private LocalDateTime appointmentStart;

  @Column(name = "next_attempt_at", nullable = false)
  private Instant nextAttemptAt;

  /** Constructs an instance for JPA. */
  protected BookingDecisionNotification() {
  }

  /** Captures the decision as it was made so it can be sent after commit. */
  public BookingDecisionNotification(String recipient, AppointmentStatus decision,
      LocalDateTime appointmentStart, Instant nextAttemptAt) {
    this.recipient = recipient;
    this.decision = decision;
    this.appointmentStart = appointmentStart;
    this.nextAttemptAt = nextAttemptAt;
  }

  /** Defers the next delivery attempt after a transport failure. */
  public void retryAt(Instant nextAttempt) {
    nextAttemptAt = nextAttempt;
  }
}
