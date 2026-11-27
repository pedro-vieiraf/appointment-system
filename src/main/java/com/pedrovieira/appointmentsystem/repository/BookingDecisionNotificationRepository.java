package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.BookingDecisionNotification;
import java.time.Instant;
import java.util.List;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

/** Persists decision emails and coordinates concurrent dispatchers. */
public interface BookingDecisionNotificationRepository
    extends JpaRepository<BookingDecisionNotification, Long> {

  /** Returns the next batch of due decision emails. */
  @Query(value = """
      select id from booking_decision_notifications
      where next_attempt_at <= :now
      order by next_attempt_at, id
      limit 100
      """, nativeQuery = true)
  List<Long> findDueIds(Instant now);

  /** Claims a due email until the caller commits or rolls back its transaction. */
  @Query(value = """
      select * from booking_decision_notifications
      where id = :id and next_attempt_at <= :now
      for update skip locked
      """, nativeQuery = true)
  Optional<BookingDecisionNotification> lockDue(long id, Instant now);
}
