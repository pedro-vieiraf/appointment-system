package com.pedrovieira.appointmentsystem.entity;

import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.BusinessRuleException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidReminderOffsetsException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.MapKeyColumn;
import jakarta.persistence.PrePersist;
import jakarta.persistence.PreUpdate;
import jakarta.persistence.Table;
import jakarta.persistence.Version;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import lombok.Getter;
import lombok.Setter;
import org.hibernate.annotations.ColumnDefault;
import org.hibernate.annotations.JdbcTypeCode;
import org.hibernate.type.SqlTypes;

/**
 * Appointments Entity.
 */
@Entity
@Table(name = "appointments")
public class Appointment {

  public static final int MAX_DURATION_MINUTES = 1440;
  private static final int DEFAULT_DURATION_MINUTES = 30;

  private static final List<Integer> DEFAULT_REMINDER_OFFSETS_MINUTES = List.of(24 * 60);

  @Getter
  @Setter
  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Version
  @Column(nullable = false)
  @ColumnDefault("0")
  private long version;

  @Getter
  @Setter
  @Column(nullable = false)
  private String title;

  @Getter
  @Setter
  private String description;

  @Getter
  @Column(nullable = false)
  private LocalDateTime appointmentDate;

  @Column(name = "duration_minutes")
  private Integer durationMinutes = DEFAULT_DURATION_MINUTES;

  @Getter
  private String guestName;

  @Getter
  private String guestEmail;

  private String bookingTimeZone;

  @Getter
  private LocalDateTime holdExpiresAt;

  @Getter
  @Column(nullable = false)
  @Enumerated(EnumType.STRING)
  private AppointmentStatus status;

  @JdbcTypeCode(SqlTypes.ARRAY)
  @Column(name = "reminder_offsets_minutes")
  private List<Integer> reminderOffsetsMinutes = new ArrayList<>(DEFAULT_REMINDER_OFFSETS_MINUTES);

  @ElementCollection
  @CollectionTable(
      name = "appointment_reminder_schedule",
      joinColumns = @JoinColumn(name = "appointment_id")
  )
  @MapKeyColumn(name = "offset_minutes")
  @Column(name = "scheduled_at", nullable = false)
  private Map<Integer, LocalDateTime> reminderSchedule = new HashMap<>();

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

  /** Changes the scheduled start without allowing a pending hold to extend past it. */
  public void setAppointmentDate(LocalDateTime date) {
    appointmentDate = date;
    if (holdExpiresAt != null && date.isBefore(holdExpiresAt)) {
      holdExpiresAt = date;
    }
  }

  /** Sets the guest details and the deadline for a pending public request. */
  public void holdForGuest(String name, String email, LocalDateTime expiresAt, String timeZone) {
    guestName = name;
    guestEmail = email;
    holdExpiresAt = expiresAt;
    bookingTimeZone = timeZone;
  }

  /** Returns the zone associated with a guest booking, defaulting to UTC for manual bookings. */
  public String getBookingTimeZone() {
    return bookingTimeZone == null ? "UTC" : bookingTimeZone;
  }

  /** Rejects changes that require an active pending request after its deadline. */
  public void requireUnexpiredHold(LocalDateTime now) {
    if (holdExpiresAt != null && !holdExpiresAt.isAfter(now)) {
      throw new BusinessRuleException(
          "O prazo para aprovar a solicitação venceu e o horário foi liberado. "
              + "Peça ao cliente para enviar uma nova solicitação.");
    }
  }

  /** Returns the appointment duration, including a default for existing records. */
  public int getDurationMinutes() {
    return durationMinutes == null ? DEFAULT_DURATION_MINUTES : durationMinutes;
  }

  /** Sets the duration in whole minutes. */
  public void changeDuration(int minutes) {
    if (minutes < 1 || minutes > MAX_DURATION_MINUTES) {
      throw new IllegalArgumentException(
          "Appointment duration must be between 1 and 1440 minutes.");
    }
    durationMinutes = minutes;
  }

  /**
   * Returns the configured offsets, using the default for appointments created before reminders.
   */
  public List<Integer> getReminderOffsetsMinutes() {
    return reminderOffsetsMinutes == null
        ? DEFAULT_REMINDER_OFFSETS_MINUTES : List.copyOf(reminderOffsetsMinutes);
  }

  /**
   * Replaces the reminder offsets. An empty list disables reminders for this appointment.
   */
  public void replaceReminderOffsets(List<Integer> offsets) {
    Set<Integer> seen = new HashSet<>();
    for (Integer offset : offsets) {
      if (offset == null || offset <= 0 || !seen.add(offset)) {
        throw new InvalidReminderOffsetsException();
      }
    }
    reminderOffsetsMinutes = new ArrayList<>(offsets);
  }

  /**
   * Plans reminders at confirmation or after a schedule change. Keeps earlier deadlines
   * for unchanged offsets so an overdue reminder can still be delivered.
   */
  public void refreshReminderSchedule(
      LocalDateTime now, boolean appointmentDateChanged, List<Integer> previousOffsets
  ) {
    if (appointmentDateChanged) {
      reminderSchedule.clear();
    }

    if (status != AppointmentStatus.CONFIRMED || !appointmentDate.isAfter(now)) {
      return;
    }

    List<Integer> offsets = getReminderOffsetsMinutes();
    reminderSchedule.keySet().retainAll(offsets);
    for (Integer offset : offsets) {
      LocalDateTime scheduledAt = appointmentDate.minusMinutes(offset);
      if ((appointmentDateChanged || !previousOffsets.contains(offset))
          && !scheduledAt.isBefore(now)) {
        reminderSchedule.putIfAbsent(offset, scheduledAt);
      }
    }
  }

  /**
   * Method to markAsConfirmed an appointment.
   */
  public void markAsConfirmed() {
    if (status != AppointmentStatus.PENDING) {
      throw new BusinessRuleException("Only pending appointments can be confirmed");
    }

    this.status = AppointmentStatus.CONFIRMED;
    holdExpiresAt = null;
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
    holdExpiresAt = null;
    reminderSchedule.clear();
  }

  /**
   * Method to conclude an appointment.
   */
  public void markAsCompleted() {
    if (status != AppointmentStatus.CONFIRMED) {
      throw new BusinessRuleException("Only confirmed appointments can be completed");
    }
    status = AppointmentStatus.COMPLETED;
    reminderSchedule.clear();
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
