package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.PublicBookingRequest;
import com.pedrovieira.appointmentsystem.dto.response.CandidateSlot;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.BookingVerification;
import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import com.pedrovieira.appointmentsystem.repository.BookingVerificationRepository;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.repository.WeeklyAvailabilityRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import lombok.RequiredArgsConstructor;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.util.UriComponentsBuilder;

/** Verifies an email address before placing a temporary hold on a public slot. */
@Service
@RequiredArgsConstructor
@ConditionalOnProperty(name = "booking.verification.enabled", havingValue = "true")
public class PublicBookingService {

  private final WeeklyAvailabilityService availabilityService;
  private final WeeklyAvailabilityRepository availabilityRepository;
  private final BookingVerificationRepository verificationRepository;
  private final AppointmentRepository appointmentRepository;
  private final UserRepository userRepository;
  private final JavaMailSender mailSender;
  private final Clock clock;

  @Value("${booking.verification.base-url}")
  private String baseUrl;

  @Value("${booking.verification.from}")
  private String from;

  @Value("${booking.verification.token-ttl:PT15M}")
  private Duration tokenTtl;

  @Value("${booking.verification.hold-ttl:PT2H}")
  private Duration holdTtl;

  /** Sends the one-time verification link without reserving the selected slot. */
  @Transactional
  public void request(UUID linkId, PublicBookingRequest request) {
    CandidateSlot slot = availabilityService.findPublicFreeSlots(linkId, request.date())
        .orElseThrow(PublicBookingException::unsharedAgenda).stream()
        .filter(candidate -> candidate.start().equals(request.start()))
        .findFirst().orElseThrow(this::unavailable);
    BookingVerification verification = verificationRepository.save(new BookingVerification(
        linkId, request.name().strip(), request.email(), request.date(), slot.start(), slot.end(),
        clock.instant().plus(tokenTtl)));

    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(request.email());
    message.setSubject("Verifique sua solicitação de agendamento");
    String link = UriComponentsBuilder.fromUriString(baseUrl)
        .pathSegment("public", "availability", linkId.toString(), "requests", "verify")
        .queryParam("token", verification.getToken())
        .build().toUriString();
    message.setText("Abra este link para verificar seu e-mail e solicitar o agendamento: " + link);
    try {
      mailSender.send(message);
    } catch (MailException exception) {
      throw new PublicBookingException(HttpStatus.SERVICE_UNAVAILABLE,
          "Não conseguimos enviar o e-mail de verificação. Tente novamente em alguns minutos.");
    }
  }

  /** Validates a verification link without consuming it or holding a slot. */
  @Transactional(readOnly = true)
  public void validateVerificationLink(UUID linkId, UUID token) {
    availabilityRepository.findByPublicLinkId(linkId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
    findVerification(linkId, token, clock.instant());
  }

  /** Rechecks the slot under lock before creating the pending appointment. */
  @Transactional
  public Instant verify(UUID linkId, UUID token) {
    long ownerId = availabilityRepository.findByPublicLinkId(linkId)
        .orElseThrow(PublicBookingException::unsharedAgenda).getUserId();
    final var owner = userRepository.findForSchedulingUpdateById(ownerId)
        .orElseThrow(PublicBookingException::unsharedAgenda);
    Instant now = clock.instant();
    BookingVerification verification = findVerification(linkId, token, now);

    appointmentRepository.expirePendingHolds(ownerId, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
    boolean available = availabilityService
        .findPublicFreeSlots(linkId, verification.getRequestedDate())
        .orElseThrow(PublicBookingException::unsharedAgenda).stream()
        .anyMatch(slot -> slot.start().equals(verification.getSlotStart())
            && slot.end().equals(verification.getSlotEnd()));
    if (!available) {
      throw unavailable();
    }

    Instant deadline = now.plus(holdTtl);
    if (deadline.isAfter(verification.getSlotStart())) {
      deadline = verification.getSlotStart();
    }

    Appointment appointment = new Appointment(owner, "Public booking request", null,
        LocalDateTime.ofInstant(verification.getSlotStart(), ZoneOffset.UTC));
    appointment.changeDuration(Math.toIntExact(
        Duration.between(verification.getSlotStart(), verification.getSlotEnd()).toMinutes()));
    appointment.holdForGuest(verification.getGuestName(), verification.getGuestEmail(),
        LocalDateTime.ofInstant(deadline, ZoneOffset.UTC));
    appointmentRepository.save(appointment);
    verificationRepository.delete(verification);
    return deadline;
  }

  private BookingVerification findVerification(UUID linkId, UUID token, Instant now) {
    return verificationRepository.findById(token)
        .filter(value -> value.getLinkId().equals(linkId) && value.getExpiresAt().isAfter(now))
        .orElseThrow(() -> new PublicBookingException(HttpStatus.NOT_FOUND,
            "Este link é inválido, venceu ou já foi usado. Solicite um novo link de verificação."));
  }

  private PublicBookingException unavailable() {
    return new PublicBookingException(HttpStatus.CONFLICT,
        "O horário escolhido não está mais disponível. Volte à agenda e escolha outro horário.");
  }
}
