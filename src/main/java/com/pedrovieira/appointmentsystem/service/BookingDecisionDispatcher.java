package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.entity.BookingDecisionNotification;
import com.pedrovieira.appointmentsystem.entity.enums.AppointmentStatus;
import com.pedrovieira.appointmentsystem.repository.BookingDecisionNotificationRepository;
import java.time.Clock;
import java.time.Duration;
import java.time.format.DateTimeFormatter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.mail.MailException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Component;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;

/** Delivers guest decision emails independently of the appointment request. */
@Component
@ConditionalOnProperty(name = "booking.notifications.enabled", havingValue = "true")
public class BookingDecisionDispatcher {

  private static final Logger LOGGER = LoggerFactory.getLogger(BookingDecisionDispatcher.class);
  private static final Duration RETRY_DELAY = Duration.ofMinutes(1);
  private static final DateTimeFormatter UTC_DATE =
      DateTimeFormatter.ofPattern("dd/MM/uuuu 'às' HH:mm 'UTC'");

  private final BookingDecisionNotificationRepository repository;
  private final JavaMailSender mailSender;
  private final TransactionTemplate transactions;
  private final Clock clock;
  private final String from;

  /** Creates a dispatcher with its own transaction for each email. */
  public BookingDecisionDispatcher(
      BookingDecisionNotificationRepository repository, JavaMailSender mailSender,
      PlatformTransactionManager transactionManager, Clock clock,
      @Value("${booking.verification.from}") String from
  ) {
    this.repository = repository;
    this.mailSender = mailSender;
    this.transactions = new TransactionTemplate(transactionManager);
    this.clock = clock;
    this.from = from;
  }

  /** Dispatches due messages and delays any failed SMTP submissions. */
  @Scheduled(fixedDelayString = "${booking.notifications.poll-interval:PT1M}")
  public void dispatch() {
    for (long id : repository.findDueIds(clock.instant())) {
      transactions.executeWithoutResult(transaction ->
          repository.lockDue(id, clock.instant()).ifPresent(this::deliver));
    }
  }

  private void deliver(BookingDecisionNotification notification) {
    try {
      mailSender.send(createMessage(notification));
      repository.delete(notification);
    } catch (MailException exception) {
      notification.retryAt(clock.instant().plus(RETRY_DELAY));
      LOGGER.warn("Could not send booking decision notification {}: {}",
          notification.getId(), exception.getClass().getSimpleName());
    }
  }

  private SimpleMailMessage createMessage(BookingDecisionNotification notification) {
    SimpleMailMessage message = new SimpleMailMessage();
    message.setFrom(from);
    message.setTo(notification.getRecipient());
    String time = notification.getAppointmentStart().format(UTC_DATE);
    if (notification.getDecision() == AppointmentStatus.CONFIRMED) {
      message.setSubject("Seu agendamento foi confirmado");
      message.setText("O profissional confirmou sua solicitação para " + time + ".");
    } else {
      message.setSubject("Sua solicitação de agendamento foi recusada");
      message.setText("O profissional recusou sua solicitação para " + time
          + ". Volte à agenda e escolha outro horário se quiser enviar um novo pedido.");
    }
    return message;
  }
}
