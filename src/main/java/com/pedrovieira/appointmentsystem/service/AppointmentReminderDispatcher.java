package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.repository.AppointmentReminderRepository;
import com.pedrovieira.appointmentsystem.repository.AppointmentReminderRepository.DueReminder;
import java.time.Clock;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.EnableScheduling;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/**
 * Sends scheduled appointment reminders to their owners.
 */
@Component
@EnableScheduling
@ConditionalOnProperty(name = "reminders.delivery-enabled", havingValue = "true")
public class AppointmentReminderDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(AppointmentReminderDispatcher.class);

  private final AppointmentReminderRepository repository;
  private final JavaMailSender mailSender;
  private final TransactionTemplate transactions;
  private final Clock clock;
  private final String fromAddress;

  /**
   * Creates a dispatcher using the application's database transaction manager.
   */
  public AppointmentReminderDispatcher(
      AppointmentReminderRepository repository,
      JavaMailSender mailSender,
      PlatformTransactionManager transactionManager,
      Clock clock,
      @Value("${reminders.from}") String fromAddress
  ) {
    this.repository = repository;
    this.mailSender = mailSender;
    this.transactions = new TransactionTemplate(transactionManager);
    this.clock = clock;
    this.fromAddress = fromAddress;
  }

  /**
   * Polls for due reminders. A failed mail send leaves that reminder queued.
   */
  @Scheduled(fixedDelayString = "${reminders.poll-interval:PT1M}")
  public void sendDueReminders() {
    for (long appointmentId : repository.findDueAppointmentIds(LocalDateTime.now(clock))) {
      try {
        while (Boolean.TRUE.equals(transactions.execute(status -> sendNext(appointmentId)))) {
          // Each successful send commits before the next reminder is checked.
        }
      } catch (MailException exception) {
        LOGGER.warn("Could not send an appointment reminder for appointment {}: {}",
            appointmentId, exception.getClass().getSimpleName());
      }
    }
  }

  private boolean sendNext(long appointmentId) {
    if (!repository.tryLockAppointment(appointmentId)) {
      return false;
    }

    return repository.findNextDueReminder(appointmentId, LocalDateTime.now(clock))
        .map(reminder -> {
          mailSender.send(createMessage(reminder));
          repository.removeDeliveredReminder(appointmentId, reminder.offsetMinutes());
          return true;
        })
        .orElse(false);
  }

  private SimpleMailMessage createMessage(DueReminder reminder) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(fromAddress);
    message.setTo(reminder.email());
    message.setSubject("Appointment reminder");
    message.setText("Reminder: " + reminder.title() + " is scheduled for "
        + reminder.appointmentDate().format(DateTimeFormatter.ISO_LOCAL_DATE_TIME) + " (UTC).");
    return message;
  }
}
