package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.WeeklyAvailabilityRequest;
import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.AvailabilityWindow;
import com.pedrovieira.appointmentsystem.entity.WeeklyAvailability;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.repository.WeeklyAvailabilityRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZoneOffset;
import java.time.zone.ZoneRules;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/**
 * Manages working hours and previews candidate appointment intervals.
 */
@Service
@RequiredArgsConstructor
public class WeeklyAvailabilityService {

  private final WeeklyAvailabilityRepository availabilityRepository;
  private final UserRepository userRepository;
  private final AppointmentRepository appointmentRepository;
  private final Clock clock;

  /**
   * Replaces the authenticated professional's schedule.
   */
  @Transactional
  public WeeklyAvailabilityRequest replace(long ownerId, WeeklyAvailabilityRequest request) {
    var owner = userRepository.findForSchedulingUpdateById(ownerId).orElseThrow();
    var availability = availabilityRepository.findById(ownerId)
        .orElseGet(() -> new WeeklyAvailability(owner));
    availability.configure(request.timeZone(), request.durationMinutes(), request.windows());
    return toSettings(availabilityRepository.save(availability));
  }

  /** Reads the configured schedule, if the account has one. */
  @Transactional(readOnly = true)
  public Optional<WeeklyAvailabilityRequest> get(long ownerId) {
    return availabilityRepository.findById(ownerId).map(this::toSettings);
  }

  /**
   * Lists future candidates on a date in the professional's configured time zone.
   */
  @Transactional(readOnly = true)
  public List<CandidateSlot> preview(long ownerId, LocalDate date) {
    return availabilityRepository.findById(ownerId)
        .map(availability -> calculateSlots(availability, date)).orElseGet(List::of);
  }

  /** Returns the current public link identifier, if sharing is enabled. */
  @Transactional(readOnly = true)
  public Optional<UUID> getPublicLink(long ownerId) {
    return availabilityRepository.findById(ownerId).map(WeeklyAvailability::getPublicLinkId);
  }

  /** Enables sharing for an existing weekly schedule. */
  @Transactional
  public Optional<UUID> enableSharing(long ownerId) {
    userRepository.findForSchedulingUpdateById(ownerId);
    return availabilityRepository.findById(ownerId).map(WeeklyAvailability::enableSharing);
  }

  /** Revokes the active public link, if one exists. */
  @Transactional
  public void revokeSharing(long ownerId) {
    userRepository.findForSchedulingUpdateById(ownerId);
    availabilityRepository.findById(ownerId).ifPresent(WeeklyAvailability::revokeSharing);
  }

  /** Returns only the available intervals for an active public link. */
  @Transactional(readOnly = true)
  public Optional<List<CandidateSlot>> findPublicFreeSlots(UUID linkId, LocalDate date) {
    return availabilityRepository.findByPublicLinkId(linkId)
        .map(availability -> findFreeSlots(availability, date));
  }

  /** Lists future slots that do not overlap an existing appointment for this owner. */
  @Transactional(readOnly = true)
  public List<CandidateSlot> findFreeSlots(long ownerId, LocalDate date) {
    return availabilityRepository.findById(ownerId)
        .map(availability -> findFreeSlots(availability, date))
        .orElseGet(List::of);
  }

  private List<CandidateSlot> findFreeSlots(WeeklyAvailability availability, LocalDate date) {
    List<CandidateSlot> candidates = calculateSlots(availability, date);
    if (candidates.isEmpty()) {
      return candidates;
    }

    LocalDateTime earliestStart = LocalDateTime.ofInstant(
        candidates.getFirst().start(), ZoneOffset.UTC)
        .minusMinutes(Appointment.MAX_DURATION_MINUTES);
    LocalDateTime latestEnd = LocalDateTime.ofInstant(candidates.getLast().end(), ZoneOffset.UTC);
    List<Appointment> appointments = appointmentRepository.findSlotOccupants(
        availability.getUserId(), earliestStart, latestEnd, AppointmentStatus.CANCELLED,
        LocalDateTime.ofInstant(clock.instant(), ZoneOffset.UTC));

    return candidates.stream()
        .filter(slot -> appointments.stream().noneMatch(appointment -> overlaps(appointment, slot)))
        .toList();
  }

  private boolean overlaps(Appointment appointment, CandidateSlot slot) {
    Instant start = appointment.getAppointmentDate().toInstant(ZoneOffset.UTC);
    Instant end = start.plus(Duration.ofMinutes(appointment.getDurationMinutes()));
    return start.isBefore(slot.end()) && end.isAfter(slot.start());
  }

  private WeeklyAvailabilityRequest toSettings(WeeklyAvailability availability) {
    return new WeeklyAvailabilityRequest(
        availability.getTimeZone(), availability.getDurationMinutes(), availability.getWindows());
  }

  private List<CandidateSlot> calculateSlots(WeeklyAvailability availability, LocalDate date) {
    Instant now = clock.instant();
    ZoneRules rules = ZoneId.of(availability.getTimeZone()).getRules();
    Duration duration = Duration.ofMinutes(availability.getDurationMinutes());
    List<CandidateSlot> slots = new ArrayList<>();

    for (AvailabilityWindow window : availability.getWindows()) {
      if (window.dayOfWeek() != date.getDayOfWeek()) {
        continue;
      }
      LocalDateTime windowEnd = date.atTime(window.end());
      for (LocalDateTime start = date.atTime(window.start());
           !start.plus(duration).isAfter(windowEnd);
           start = start.plus(duration)) {
        LocalDateTime end = start.plus(duration);
        var startOffsets = rules.getValidOffsets(start);
        var endOffsets = rules.getValidOffsets(end);
        if (startOffsets.size() == 1 && startOffsets.equals(endOffsets)) {
          Instant slotStart = start.toInstant(startOffsets.getFirst());
          if (slotStart.isAfter(now)) {
            slots.add(new CandidateSlot(slotStart, end.toInstant(endOffsets.getFirst())));
          }
        }
      }
    }
    return slots;
  }
}
