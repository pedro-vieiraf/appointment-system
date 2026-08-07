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
  private LocalDateTime startAt;

  @Getter
  @Setter
  @Column(nullable = false)
  private LocalDateTime endAt;

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

  public Appointment() {
  }

  public Appointment(String title, LocalDateTime startAt, LocalDateTime endAt,
      AppointmentStatus status, User user) {
    this.title = title;
    this.startAt = startAt;
    this.endAt = endAt;
    this.status = status;
    this.user = user;
  }

  public void cancel() {
    status = AppointmentStatus.CANCELLED;
  }

  public void complete() {
    status = AppointmentStatus.COMPLETED;
  }
}
