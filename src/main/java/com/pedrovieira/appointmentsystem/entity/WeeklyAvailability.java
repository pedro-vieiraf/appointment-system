package com.pedrovieira.appointmentsystem.entity;

import com.pedrovieira.appointmentsystem.exception.InvalidAvailabilityException;
import jakarta.persistence.CollectionTable;
import jakarta.persistence.Column;
import jakarta.persistence.ElementCollection;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.MapsId;
import jakarta.persistence.OneToOne;
import jakarta.persistence.Table;
import java.time.DateTimeException;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.UUID;
import lombok.Getter;

/**
 * Weekly working hours and service duration for one professional.
 */
@Entity
@Table(name = "weekly_availability")
public class WeeklyAvailability {

  @Getter
  @Id
  @Column(name = "user_id")
  private Long userId;

  @MapsId
  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id")
  private User owner;

  @Getter
  @Column(nullable = false)
  private String timeZone;

  @Getter
  @Column(nullable = false)
  private int durationMinutes;

  @Getter
  @Column(name = "public_link_id", unique = true)
  private UUID publicLinkId;

  @ElementCollection
  @CollectionTable(
      name = "weekly_availability_windows", joinColumns = @JoinColumn(name = "user_id"))
  private List<AvailabilityWindow> windows = new ArrayList<>();

  /** Constructs an instance for JPA. */
  protected WeeklyAvailability() {
  }

  /** Creates a schedule for one account. */
  public WeeklyAvailability(User owner) {
    this.owner = owner;
  }

  /**
   * Replaces the weekly schedule after validating the full set of working hours.
   */
  public void configure(String zone, int duration, List<AvailabilityWindow> newWindows) {
    ZoneId parsedZone;
    try {
      parsedZone = ZoneId.of(zone);
    } catch (DateTimeException exception) {
      throw new InvalidAvailabilityException("Use a valid time zone identifier.");
    }

    final List<AvailabilityWindow> ordered = order(newWindows);
    AvailabilityWindow previous = null;
    for (AvailabilityWindow window : ordered) {
      if (!window.start().isBefore(window.end())) {
        throw new InvalidAvailabilityException("Working hours must start before they end.");
      }
      if (previous != null && previous.dayOfWeek() == window.dayOfWeek()
          && window.start().isBefore(previous.end())) {
        throw new InvalidAvailabilityException("Working hours on the same day may not overlap.");
      }
      previous = window;
    }

    timeZone = parsedZone.getId();
    durationMinutes = duration;
    windows.clear();
    windows.addAll(ordered);
  }

  /** Enables sharing and returns the existing link if sharing is already enabled. */
  public UUID enableSharing() {
    if (publicLinkId == null) {
      publicLinkId = UUID.randomUUID();
    }
    return publicLinkId;
  }

  /** Invalidates the current public link. */
  public void revokeSharing() {
    publicLinkId = null;
  }

  /** Returns the windows ordered by weekday and local start time. */
  public List<AvailabilityWindow> getWindows() {
    return order(windows);
  }

  private static List<AvailabilityWindow> order(List<AvailabilityWindow> windows) {
    return windows.stream().sorted(Comparator.comparing(AvailabilityWindow::dayOfWeek)
        .thenComparing(AvailabilityWindow::start)).toList();
  }
}
