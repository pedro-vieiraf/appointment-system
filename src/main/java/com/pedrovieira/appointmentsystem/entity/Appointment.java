package com.pedrovieira.appointmentsystem.entity;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import lombok.Getter;
import lombok.Setter;

/**
 * Appointments Entity.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

  @Getter
  @Setter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  @Setter
  @Column(nullable = false)
  private String title;

  @Getter
  @Setter
  private String description;

  @Getter
  @Setter
  @Column(nullable = false)
  private LocalDateTime appointmentDate;

  @Getter
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Getter
  @ManyToOne(fetch = FetchType.LAZY)
  @JoinColumn(
      name = "user_id",
      nullable = false
  )
  private User user;

  /**
   * Empty constructor for JPA.
   */
  public Appointment() {
  }

  /**
   * Standard constructor for appointments.
   */
  public Appointment(
      User user,
      String title,
      String description,
      LocalDateTime appointmentDate
  ) {
    if (title == null || title.isBlank()) {
      throw new IllegalArgumentException("Title is required");
    }

    if (appointmentDate == null) {
      throw new IllegalArgumentException("Appointment date is required");
    }

    this.user = user;
    this.title = title;
    this.description = description;
    this.appointmentDate = appointmentDate;
    this.status = AppointmentStatus.PENDING;
  }

  /**
   * Constructor for optional description.
   */
  public Appointment(
      User user,
      String title,
      LocalDateTime appointmentDate
  ) {
    this(
        user,
        title,
        "No description provided",
        appointmentDate
    );
  }

  /**
   * Method to markAsConfirmed an appointment.
   */
  public void markAsConfirmed() {
    if (status != AppointmentStatus.PENDING) {
      throw new BusinessRuleException("Only pending appointments can be confirmed");
    }

    this.status = AppointmentStatus.CONFIRMED;
  }

  /**
   * Method to markAsCanceled an appointment.
   */
  public void markAsCanceled() {
    if (status != AppointmentStatus.CONFIRMED
        && status != AppointmentStatus.PENDING) {
      throw new BusinessRuleException("Only pending or confirmed appointments can be cancelled");
    }
    status = AppointmentStatus.CANCELLED;
  }

  /**
   * Method to conclude an appointment.
   */
  public void markAsCompleted() {
    if (status != AppointmentStatus.CONFIRMED) {
      throw new BusinessRuleException("Only confirmed appointments can be completed");
    }
    status = AppointmentStatus.COMPLETED;
  }

  @PrePersist
  private void prePersist() {
    LocalDateTime now = LocalDateTime.now();

    createdAt = now;
    updatedAt = now;
  }

  @PreUpdate
  private void preUpdate() {
    updatedAt = LocalDateTime.now();
  }
}
