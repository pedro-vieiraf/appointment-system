package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.repository.AppointmentRepository;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import lombok.RequiredArgsConstructor;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

/** Releases expired booking holds and removes expired request counters. */
@Service
@RequiredArgsConstructor
public class BookingExpirationService {

  private final AppointmentRepository appointmentRepository;
  private final BookingRequestLimiter requestLimiter;
  private final Clock clock;

  /** Expires pending requests that the professional can no longer approve. */
  @Scheduled(fixedDelayString = "${booking.expiration.poll-interval:PT1M}")
  @Transactional
  public void expire() {
    var now = clock.instant();
    appointmentRepository.expirePendingHolds(null, LocalDateTime.ofInstant(now, ZoneOffset.UTC));
    requestLimiter.deleteExpired(now);
  }
}
