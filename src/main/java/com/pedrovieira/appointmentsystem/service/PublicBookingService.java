package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.PublicBookingRequest;
import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.repository.WeeklyAvailabilityRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.Locale;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Places a temporary hold on an available public slot. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.requests.enabled", havingValue = "true")
public class PublicBookingService {

  private final WeeklyAvailabilityService availabilityService;
  private final WeeklyAvailabilityRepository availabilityRepository;
  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final BookingRequestLimiter requestLimiter;
  private final Clock clock;

  @Value("${booking.requests.hold-ttl:PT2H}")
  private Duration holdTtl;

  /** Counts a request before opening the booking transaction. */
  public void checkRateLimit(UUID linkId, String address) {
    long ownerId = findOwnerId(linkId);
    requestLimiter.check(ownerId, address);
  }

  /** Saves a pending request after rechecking the slot under the professional's lock. */
  @Transactional
  public Instant request(UUID linkId, PublicBookingRequest request) {
    long ownerId = findOwnerId(linkId);
    final var owner = userRepository.findForSchedulingUpdateById(ownerId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
    final Instant now = clock.instant();
    final String email = request.email().strip().toLowerCase(Locale.ROOT);
    appointmentRepository.expirePendingHolds(ownerId, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
    if (appointmentRepository.existsByUserIdAndGuestEmailIgnoreCaseAndStatusAndHoldExpiresAtAfter(
        ownerId, email, AppointmentStatus.PENDING, LocalDateTime.ofInstant(now, ZoneOffset.UTC))) {
      throw new PublicBookingException(HttpStatus.CONFLICT,
          "Já existe uma solicitação pendente com esse e-mail nesta agenda. Aguarde a resposta.");
    }

    final var availability = availabilityRepository.findByPublicLinkId(linkId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
    CandidateSlot slot = availabilityService.findPublicFreeSlots(linkId, request.date())
        .orElseThrow(PublicBookingException::unsharedAgenda).stream()
        .filter(candidate -> candidate.start().equals(request.start()))
        .findFirst().orElseThrow(this::unavailable);

    Instant deadline = now.plus(holdTtl);
    if (deadline.isAfter(slot.start())) {
      deadline = slot.start();
    }
    Appointment appointment = new Appointment(owner, "Public booking request", null,
        LocalDateTime.ofInstant(slot.start(), ZoneOffset.UTC));
    appointment.changeDuration(Math.toIntExact(
        Duration.between(slot.start(), slot.end()).toMinutes()));
    appointment.holdForGuest(request.name().strip(), email,
        LocalDateTime.ofInstant(deadline, ZoneOffset.UTC), availability.getTimeZone());
    appointmentRepository.save(appointment);
    return deadline;
  }

  private long findOwnerId(UUID linkId) {
    return availabilityRepository.findPublicOwnerId(linkId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
  }

  private PublicBookingException unavailable() {
    return new PublicBookingException(HttpStatus.CONFLICT,
        "O horário escolhido não está mais disponível. Volte à agenda e escolha outro horário.");
  }
}
