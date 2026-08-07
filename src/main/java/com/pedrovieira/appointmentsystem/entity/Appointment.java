package com.pedrovieira.appointmentsystem.entity;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
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
  @Setter
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  @Column(nullable = false)
  private LocalDateTime createdAt;

  @Column(nullable = false)
  private LocalDateTime updatedAt;

  @Getter
  @Setter
  @ManyToOne
  @JoinColumn(name = "user_id")
  private User user;

  /**
   * Empty constructor for JPA.
   */
  public Appointment() {
  }

  /**
   * Standard constructor for appointments.
   */
  public Appointment(User user, String title, LocalDateTime appointmentDate) {
    this.user = user;
    this.title = title;
    this.appointmentDate = appointmentDate;
    this.status = AppointmentStatus.PENDING;

  }

  /**
   * Method to markAsConfirmed an appointment.
   */
  public void markAsConfirmed() {
    if (status != AppointmentStatus.PENDING) {
      throw new IllegalStateException("Status must be PENDING");
    }

    this.status = AppointmentStatus.CONFIRMED;
  }

  /**
   * Method to markAsCanceled an appointment.
   */
  public void markAsCanceled() {
    status = AppointmentStatus.CANCELLED;
  }

  /**
   * Method to conclude an appointment.
   */
  public void markAsCompleted() {
    status = AppointmentStatus.COMPLETED;
  }

  public LocalDateTime getAppointmentDate() {
    return appointmentDate;
  }
}
