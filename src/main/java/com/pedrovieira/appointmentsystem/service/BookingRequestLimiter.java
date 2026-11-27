package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.exception.PublicBookingException;
import java.time.Clock;
import java.time.Duration;
import java.time.Instant;
import java.time.OffsetDateTime;
import java.time.ZoneOffset;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;

/** Applies a shared fixed-window limit per professional and client address. */
@Service
public class BookingRequestLimiter {

  private final JdbcTemplate jdbc;
  private final Clock clock;
  private final int limit;
  private final Duration window;

  /** Creates a database-backed rate limiter. */
  public BookingRequestLimiter(JdbcTemplate jdbc, Clock clock,
      @Value("${booking.requests.rate-limit:5}") int limit,
      @Value("${booking.requests.rate-window:PT15M}") Duration window) {
    if (limit < 1 || limit == Integer.MAX_VALUE || window.isNegative() || window.isZero()) {
      throw new IllegalArgumentException("Booking rate limit and window must be positive.");
    }
    this.jdbc = jdbc;
    this.clock = clock;
    this.limit = limit;
    this.window = window;
  }

  /**
   * Records an attempt independently so conflicts and validation errors in booking do not undo it.
   */
  @Transactional(propagation = Propagation.REQUIRES_NEW,
      noRollbackFor = PublicBookingException.class)
  public void check(long ownerId, String address) {
    OffsetDateTime now = clock.instant().atOffset(ZoneOffset.UTC);
    jdbc.query("""
        insert into booking_request_limits (owner_id, client_address, expires_at, request_count)
        values (?, ?, ?, 1)
        on conflict (owner_id, client_address) do update set
          request_count = case when booking_request_limits.expires_at <= ?
            then 1 else least(booking_request_limits.request_count, ?) + 1 end,
          expires_at = case when booking_request_limits.expires_at <= ?
            then excluded.expires_at else booking_request_limits.expires_at end
        returning request_count, expires_at
        """, row -> {
          if (row.next() && row.getInt("request_count") > limit) {
            Duration remaining = Duration.between(
                now, row.getObject("expires_at", OffsetDateTime.class));
            long seconds = remaining.toSeconds() + (remaining.getNano() == 0 ? 0 : 1);
            throw new PublicBookingException(HttpStatus.TOO_MANY_REQUESTS,
                "Você enviou muitas solicitações. Aguarde antes de tentar novamente.", seconds);
          }
          return null;
        }, ownerId, address, now.plus(window), now, limit, now);
  }

  /** Removes expired counters. */
  public void deleteExpired(Instant now) {
    jdbc.update("delete from booking_request_limits where expires_at <= ?",
        now.atOffset(ZoneOffset.UTC));
  }
}
