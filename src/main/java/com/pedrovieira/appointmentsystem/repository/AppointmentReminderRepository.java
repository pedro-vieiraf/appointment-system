package com.pedrovieira.appointmentsystem.repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import lombok.RequiredArgsConstructor;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

/**
 * Reads and consumes due reminders from the appointment schedule.
 */
@Repository
@RequiredArgsConstructor
public class AppointmentReminderRepository {

  private final JdbcTemplate jdbcTemplate;

  /**
   * Lists candidate appointment IDs in order of their earliest due reminder.
   */
  public List<Long> findDueAppointmentIds(LocalDateTime now) {
    return jdbcTemplate.queryForList("""
        select a.id
        from appointments a
        join appointment_reminder_schedule r on r.appointment_id = a.id
        where a.status = 'CONFIRMED' and a.appointment_date > ? and r.scheduled_at <= ?
        group by a.id
        order by min(r.scheduled_at), a.id
        """, Long.class, now, now);
  }

  /**
   * Claims an appointment in the caller's transaction and skips appointments being processed.
   */
  public boolean tryLockAppointment(long appointmentId) {
    return !jdbcTemplate.queryForList("""
        select id from appointments where id = ? for update skip locked
        """, Long.class, appointmentId).isEmpty();
  }

  /**
   * Rechecks delivery eligibility after the appointment has been locked.
   */
  public Optional<DueReminder> findNextDueReminder(long appointmentId, LocalDateTime now) {
    return jdbcTemplate.query("""
        select r.offset_minutes, u.email, a.title, a.appointment_date
        from appointments a
        join appointment_reminder_schedule r on r.appointment_id = a.id
        join users u on u.id = a.user_id
        where a.id = ? and a.status = 'CONFIRMED'
          and a.appointment_date > ? and r.scheduled_at <= ?
        order by r.scheduled_at, r.offset_minutes
        limit 1
        for update of r skip locked
        """, (row, index) -> new DueReminder(
        row.getInt("offset_minutes"), row.getString("email"), row.getString("title"),
        row.getObject("appointment_date", LocalDateTime.class)), appointmentId, now, now)
        .stream()
        .findFirst();
  }

  /**
   * Removes a reminder after its message has been accepted by the mail transport.
   */
  public void removeDeliveredReminder(long appointmentId, int offsetMinutes) {
    jdbcTemplate.update("""
        delete from appointment_reminder_schedule
        where appointment_id = ? and offset_minutes = ?
        """, appointmentId, offsetMinutes);
  }

  /**
   * Data used to send a single reminder.
   */
  public record DueReminder(
      int offsetMinutes, String email, String title, LocalDateTime appointmentDate
  ) {
  }
}
