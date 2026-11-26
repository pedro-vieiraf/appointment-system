package com.pedrovieira.appointmentsystem;

import static org.hamcrest.Matchers.contains;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.doAnswer;
import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.times;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.dto.response.AppointmentResponse;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import com.pedrovieira.appointmentsystem.repository.AppointmentReminderRepository;
import com.pedrovieira.appointmentsystem.service.AppointmentReminderDispatcher;
import java.nio.charset.StandardCharsets;
import java.sql.SQLException;
import java.time.Clock;
import java.time.Duration;
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.UUID;
import java.util.concurrent.Callable;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.core.io.ClassPathResource;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.mail.MailSendException;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.PlatformTransactionManager;
import org.springframework.transaction.support.TransactionTemplate;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "jwt.expiration=15m")
@AutoConfigureMockMvc
class AppointmentFlowIntegrationTest {

  private static final String PASSWORD = "appointment-test-password";

  private final String ownerEmail = UUID.randomUUID() + "@example.test";
  private final String otherEmail = UUID.randomUUID() + "@example.test";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private JdbcTemplate jdbcTemplate;

  @Autowired
  private AppointmentReminderRepository reminderRepository;

  @Autowired
  private PlatformTransactionManager transactionManager;

  @MockitoSpyBean
  private Clock clock;

  @AfterEach
  void deleteTestRecords() {
    jdbcTemplate.update("""
        delete from appointment_reminder_schedule
        where appointment_id in (
          select a.id from appointments a join users u on u.id = a.user_id
          where u.email in (?, ?)
        )
        """, ownerEmail, otherEmail);
    jdbcTemplate.update("""
        delete from appointments
        where user_id in (select id from users where email in (?, ?))
        """, ownerEmail, otherEmail);
    jdbcTemplate.update("delete from users where email in (?, ?)", ownerEmail, otherEmail);
  }

  @Test
  void ownerCanCreateListAndConfirmAppointmentWhileAnotherUserCannotAccessIt()
      throws Exception {
    String ownerAuthorization = registerAndLogin("Appointment Owner", ownerEmail);
    String otherAuthorization = registerAndLogin("Other Account", otherEmail);

    long appointmentId = createAppointment(ownerAuthorization);

    mockMvc.perform(get("/appointments")
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.totalElements").value(1))
        .andExpect(jsonPath("$.content[0].id").value(appointmentId));

    mockMvc.perform(get("/appointments")
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.content").isEmpty());

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isNotFound());

    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isNotFound());

    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
  }

  @Test
  void onlyOwnerCanDeleteAppointment() throws Exception {
    String ownerAuthorization = registerAndLogin("Appointment Owner", ownerEmail);
    String otherAuthorization = registerAndLogin("Other Account", otherEmail);
    long appointmentId = createAppointment(ownerAuthorization);

    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk());
    assertEquals(1, scheduledTimes(appointmentId).size());

    mockMvc.perform(delete("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isNoContent());

    assertEquals(List.of(), scheduledTimes(appointmentId));

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isNotFound());
  }

  @Test
  void completingPendingAppointmentReturnsConflictAndPreservesStatus() throws Exception {
    String ownerAuthorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(ownerAuthorization);

    mockMvc.perform(patch("/appointments/{id}/complete", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isConflict());

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("PENDING"));
  }

  @Test
  void storesDefaultAndCustomRemindersAndAllowsDisablingThem() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization);
    LocalDateTime date = LocalDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").value(contains(1440)));

    mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(Map.of(
                "title", "No reminders", "appointmentDate", date,
                "reminderOffsetsMinutes", new int[0]))))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").isEmpty());

    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reminderOffsetsMinutes":[1440,120]}
                """))
        .andExpect(status().isOk());

    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"Updated math lesson"}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").value(contains(1440, 120)));

    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reminderOffsetsMinutes":[]}
                """))
        .andExpect(status().isOk());

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").isEmpty());
  }

  @Test
  void legacyAppointmentWithoutStoredOffsetsUsesDefaultReminder() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization);
    jdbcTemplate.update("update appointments set reminder_offsets_minutes = null where id = ?",
        appointmentId);

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").value(contains(1440)));
  }

  @ParameterizedTest
  @ValueSource(strings = {"[0]", "[-5]", "[null]", "[120,120]"})
  void rejectsInvalidReminderOffsetsOnCreateAndUpdate(String offsets) throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization);
    LocalDateTime date = LocalDateTime.now(ZoneOffset.UTC).plusDays(3).withNano(0);

    mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"Invalid reminders","appointmentDate":"%s","reminderOffsetsMinutes":%s}
                """.formatted(date, offsets)))
        .andExpect(status().isBadRequest());

    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reminderOffsetsMinutes":%s}
                """.formatted(offsets)))
        .andExpect(status().isBadRequest());

    mockMvc.perform(get("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.reminderOffsetsMinutes").value(contains(1440)));
  }

  @Test
  void schedulesOnConfirmationAndUpdatesThePlanWithoutLosingOverdueReminders()
      throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    LocalDateTime appointmentDate = now.plusHours(4);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization,
        new AppointmentRequest("Math lesson", "Review algebra", appointmentDate,
            List.of(1440, 120, 30)));

    assertEquals(List.of(), scheduledTimes(appointmentId));

    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());
    assertEquals(List.of(appointmentDate.minusHours(2), appointmentDate.minusMinutes(30)),
        scheduledTimes(appointmentId));

    doReturn(now.plusHours(3).toInstant(ZoneOffset.UTC)).when(clock).instant();
    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"reminderOffsetsMinutes":[120,15]}
                """))
        .andExpect(status().isOk());
    assertEquals(List.of(appointmentDate.minusHours(2), appointmentDate.minusMinutes(15)),
        scheduledTimes(appointmentId));

    LocalDateTime newDate = appointmentDate.plusDays(1);
    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(Map.of("appointmentDate", newDate))))
        .andExpect(status().isOk());
    assertEquals(List.of(newDate.minusHours(2), newDate.minusMinutes(15)),
        scheduledTimes(appointmentId));

    mockMvc.perform(patch("/appointments/{id}/cancel", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());
    assertEquals(List.of(), scheduledTimes(appointmentId));
  }

  @Test
  void retriesLateReminderAfterMailFailureAndDoesNotRepeatSuccessfulDelivery() throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    LocalDateTime appointmentDate = now.plusHours(4);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization,
        new AppointmentRequest("Math lesson", "Review algebra", appointmentDate, List.of(120)));
    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());

    JavaMailSender sender = mock(JavaMailSender.class);
    AppointmentReminderDispatcher dispatcher = new AppointmentReminderDispatcher(
        reminderRepository, sender, transactionManager, clock, "reminders@example.test");

    dispatcher.sendDueReminders();
    verifyNoInteractions(sender);

    doReturn(now.plusHours(3).toInstant(ZoneOffset.UTC)).when(clock).instant();
    doThrow(new MailSendException("SMTP temporarily unavailable")).doNothing()
        .when(sender).send(any(SimpleMailMessage.class));

    dispatcher.sendDueReminders();
    assertEquals(List.of(appointmentDate.minusHours(2)), scheduledTimes(appointmentId));

    dispatcher.sendDueReminders();
    dispatcher.sendDueReminders();
    assertEquals(List.of(), scheduledTimes(appointmentId));

    ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender, times(2)).send(messages.capture());
    assertArrayEquals(new String[]{ownerEmail}, messages.getValue().getTo());
    assertEquals("Appointment reminder", messages.getValue().getSubject());
  }

  @Test
  void ignoresCancelledAndExpiredAppointmentsWhenPolling() throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long expiredId = createAppointment(authorization, new AppointmentRequest(
        "Past lesson", "Review algebra", now.plusHours(2), List.of(60)));
    long cancelledId = createAppointment(authorization, new AppointmentRequest(
        "Cancelled lesson", "Review algebra", now.plusHours(4), List.of(120)));

    for (long id : List.of(expiredId, cancelledId)) {
      mockMvc.perform(patch("/appointments/{id}/confirm", id)
              .header(HttpHeaders.AUTHORIZATION, authorization))
          .andExpect(status().isOk());
    }
    mockMvc.perform(patch("/appointments/{id}/cancel", cancelledId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());

    doReturn(now.plusHours(3).toInstant(ZoneOffset.UTC)).when(clock).instant();
    JavaMailSender sender = mock(JavaMailSender.class);
    new AppointmentReminderDispatcher(
        reminderRepository, sender, transactionManager, clock, "reminders@example.test")
        .sendDueReminders();

    verifyNoInteractions(sender);
  }

  @Test
  void concurrentDispatchersSendTheSameDueReminderOnlyOnce() throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    LocalDateTime appointmentDate = now.plusHours(4);
    LocalDateTime scheduledAt = appointmentDate.minusHours(2);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();

    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization,
        new AppointmentRequest("Math lesson", "Review algebra", appointmentDate, List.of(120)));
    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());

    doReturn(scheduledAt.toInstant(ZoneOffset.UTC)).when(clock).instant();
    assertEquals(List.of(scheduledAt), scheduledTimes(appointmentId));

    JavaMailSender firstSender = mock(JavaMailSender.class);
    JavaMailSender secondSender = mock(JavaMailSender.class);
    AppointmentReminderDispatcher firstDispatcher = new AppointmentReminderDispatcher(
        reminderRepository, firstSender, transactionManager, clock, "reminders@example.test");
    AppointmentReminderDispatcher secondDispatcher = new AppointmentReminderDispatcher(
        reminderRepository, secondSender, transactionManager, clock, "reminders@example.test");

    CountDownLatch firstSendStarted = new CountDownLatch(1);
    CountDownLatch allowFirstSendToFinish = new CountDownLatch(1);
    doAnswer(invocation -> {
      firstSendStarted.countDown();
      assertTrue(allowFirstSendToFinish.await(20, TimeUnit.SECONDS),
          "The first send was not released");
      return null;
    }).when(firstSender).send(any(SimpleMailMessage.class));

    try (var executor = Executors.newFixedThreadPool(2)) {
      var firstPoll = executor.submit(firstDispatcher::sendDueReminders);
      try {
        assertTrue(firstSendStarted.await(10, TimeUnit.SECONDS),
            "The first dispatcher did not reach the mail sender");
        executor.submit(secondDispatcher::sendDueReminders).get(10, TimeUnit.SECONDS);

        verifyNoInteractions(secondSender);
        assertEquals(List.of(scheduledAt), scheduledTimes(appointmentId));
      } finally {
        allowFirstSendToFinish.countDown();
      }
      firstPoll.get(10, TimeUnit.SECONDS);
    }

    verify(firstSender).send(any(SimpleMailMessage.class));
    assertEquals(List.of(), scheduledTimes(appointmentId));

    secondDispatcher.sendDueReminders();
    verifyNoInteractions(secondSender);
  }

  @Test
  void concurrentDispatchersSendBothRemindersDueAtTheSameTime() throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    LocalDateTime scheduledAt = now.plusHours(1);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();

    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long firstId = createAppointment(authorization, new AppointmentRequest(
        "Two-hour appointment", "Review algebra", scheduledAt.plusHours(2), List.of(120)));
    long secondId = createAppointment(authorization, new AppointmentRequest(
        "Twenty-four-hour appointment", "Review geometry", scheduledAt.plusHours(24),
        List.of(1440)));
    for (long id : List.of(firstId, secondId)) {
      mockMvc.perform(patch("/appointments/{id}/confirm", id)
              .header(HttpHeaders.AUTHORIZATION, authorization))
          .andExpect(status().isOk());
      assertEquals(List.of(scheduledAt), scheduledTimes(id));
    }

    doReturn(scheduledAt.toInstant(ZoneOffset.UTC)).when(clock).instant();
    JavaMailSender firstSender = mock(JavaMailSender.class);
    JavaMailSender secondSender = mock(JavaMailSender.class);
    AppointmentReminderDispatcher firstDispatcher = new AppointmentReminderDispatcher(
        reminderRepository, firstSender, transactionManager, clock, "reminders@example.test");
    AppointmentReminderDispatcher secondDispatcher = new AppointmentReminderDispatcher(
        reminderRepository, secondSender, transactionManager, clock, "reminders@example.test");

    CountDownLatch firstSendStarted = new CountDownLatch(1);
    CountDownLatch allowFirstSendToFinish = new CountDownLatch(1);
    doAnswer(invocation -> {
      firstSendStarted.countDown();
      assertTrue(allowFirstSendToFinish.await(20, TimeUnit.SECONDS),
          "The first send was not released");
      return null;
    }).when(firstSender).send(any(SimpleMailMessage.class));

    try (var executor = Executors.newFixedThreadPool(2)) {
      var firstPoll = executor.submit(firstDispatcher::sendDueReminders);
      try {
        assertTrue(firstSendStarted.await(10, TimeUnit.SECONDS),
            "The first dispatcher did not reach the mail sender");
        executor.submit(secondDispatcher::sendDueReminders).get(10, TimeUnit.SECONDS);

        assertEquals(List.of(scheduledAt), scheduledTimes(firstId));
        assertEquals(List.of(), scheduledTimes(secondId));
      } finally {
        allowFirstSendToFinish.countDown();
      }
      firstPoll.get(10, TimeUnit.SECONDS);
    }

    ArgumentCaptor<SimpleMailMessage> firstMessage = ArgumentCaptor.forClass(SimpleMailMessage.class);
    ArgumentCaptor<SimpleMailMessage> secondMessage = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(firstSender).send(firstMessage.capture());
    verify(secondSender).send(secondMessage.capture());
    assertTrue(firstMessage.getValue().getText().contains("Two-hour appointment"));
    assertTrue(secondMessage.getValue().getText().contains("Twenty-four-hour appointment"));
    assertArrayEquals(new String[]{ownerEmail}, firstMessage.getValue().getTo());
    assertArrayEquals(new String[]{ownerEmail}, secondMessage.getValue().getTo());
    assertEquals(List.of(), scheduledTimes(firstId));
    assertEquals(List.of(), scheduledTimes(secondId));
  }

  @Test
  void reschedulingAfterDeliverySendsAnotherReminderForTheNewTime() throws Exception {
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    LocalDateTime originalDate = now.plusHours(4);
    doReturn(now.toInstant(ZoneOffset.UTC)).when(clock).instant();

    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    long appointmentId = createAppointment(authorization, new AppointmentRequest(
        "Math lesson", "Review algebra", originalDate, List.of(120)));
    mockMvc.perform(patch("/appointments/{id}/confirm", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());

    JavaMailSender sender = mock(JavaMailSender.class);
    AppointmentReminderDispatcher dispatcher = new AppointmentReminderDispatcher(
        reminderRepository, sender, transactionManager, clock, "reminders@example.test");
    doReturn(originalDate.minusHours(2).toInstant(ZoneOffset.UTC)).when(clock).instant();
    dispatcher.sendDueReminders();
    verify(sender).send(any(SimpleMailMessage.class));
    assertEquals(List.of(), scheduledTimes(appointmentId));

    LocalDateTime newDate = originalDate.plusDays(1);
    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(Map.of("appointmentDate", newDate))))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.status").value("CONFIRMED"));
    assertEquals(List.of(newDate.minusHours(2)), scheduledTimes(appointmentId));
    dispatcher.sendDueReminders();
    verify(sender).send(any(SimpleMailMessage.class));

    doReturn(newDate.minusHours(2).toInstant(ZoneOffset.UTC)).when(clock).instant();
    dispatcher.sendDueReminders();
    dispatcher.sendDueReminders();

    ArgumentCaptor<SimpleMailMessage> messages = ArgumentCaptor.forClass(SimpleMailMessage.class);
    verify(sender, times(2)).send(messages.capture());
    assertTrue(messages.getAllValues().get(0).getText()
        .contains(originalDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    assertTrue(messages.getAllValues().get(1).getText()
        .contains(newDate.format(DateTimeFormatter.ISO_LOCAL_DATE_TIME)));
    assertEquals(List.of(), scheduledTimes(appointmentId));
  }

  @Test
  void concurrentCreatesAllowOnlyOneAppointmentInTheSameSlot() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime slot = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    String request = jsonMapper.writeValueAsString(
        new AppointmentRequest("Math lesson", null, slot));

    Callable<Integer> create = () -> mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andReturn().getResponse().getStatus();

    assertEquals(List.of(201, 409), runCompetingWrites(create, create));
    assertEquals(1, appointmentsAt(ownerEmail, slot));
  }

  @Test
  void concurrentReschedulesAllowOnlyOneOfTwoOverlappingIntervals() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime slot = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    LocalDateTime firstDate = slot.minusDays(1);
    LocalDateTime secondDate = slot.plusDays(1);
    long firstId = createAppointment(authorization,
        new AppointmentRequest("Math lesson", null, firstDate));
    long secondId = createAppointment(authorization,
        new AppointmentRequest("Geometry lesson", null, secondDate));
    String firstRequest = jsonMapper.writeValueAsString(Map.of("appointmentDate", slot));
    String secondRequest = jsonMapper.writeValueAsString(
        Map.of("appointmentDate", slot.plusMinutes(15)));

    assertEquals(List.of(200, 409), runCompetingWrites(
        () -> rescheduleStatus(authorization, firstId, firstRequest),
        () -> rescheduleStatus(authorization, secondId, secondRequest)));
    assertEquals(1, appointmentsAt(ownerEmail, slot)
        + appointmentsAt(ownerEmail, slot.plusMinutes(15)));
    assertEquals(1, appointmentsAt(ownerEmail, firstDate) + appointmentsAt(ownerEmail, secondDate));
  }

  @Test
  void cancellationFreesTheSlotForCreationAndRescheduling() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    String otherAuthorization = registerAndLogin("Other Account", otherEmail);
    LocalDateTime slot = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    AppointmentRequest request = new AppointmentRequest("Math lesson", null, slot);
    long originalId = createAppointment(authorization, request);
    createAppointment(otherAuthorization, request);
    cancelAppointment(authorization, originalId);

    long replacementId = createAppointment(authorization, request);
    cancelAppointment(authorization, replacementId);
    long movedId = createAppointment(authorization,
        new AppointmentRequest("Geometry lesson", null, slot.plusDays(1)));

    assertEquals(200, rescheduleStatus(authorization, movedId,
        jsonMapper.writeValueAsString(Map.of("appointmentDate", slot))));
    assertEquals(1, appointmentsAt(ownerEmail, slot));
    assertEquals(1, appointmentsAt(otherEmail, slot));
  }

  @Test
  void concurrentCancellationAndRescheduleCannotOverwriteEachOther() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime originalDate = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    LocalDateTime newDate = originalDate.plusDays(1);
    long id = createAppointment(authorization,
        new AppointmentRequest("Math lesson", null, originalDate));
    String request = jsonMapper.writeValueAsString(Map.of("appointmentDate", newDate));

    Callable<Integer> cancel = () -> mockMvc.perform(patch("/appointments/{id}/cancel", id)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andReturn().getResponse().getStatus();

    assertEquals(List.of(200, 409), runCompetingWrites(
        cancel, () -> rescheduleStatus(authorization, id, request)));

    String response = mockMvc.perform(get("/appointments/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    AppointmentResponse appointment = jsonMapper.readValue(response, AppointmentResponse.class);
    LocalDateTime winningDate = switch (appointment.status()) {
      case CANCELLED -> originalDate;
      case PENDING -> newDate;
      default -> throw new AssertionError("Unexpected appointment status");
    };
    assertEquals(winningDate, appointment.appointmentDate());
  }

  @Test
  void rescheduleRejectsADateInThePastAccordingToTheApplicationClock() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime now = LocalDateTime.now(ZoneOffset.UTC).withNano(0);
    long id = createAppointment(authorization,
        new AppointmentRequest("Math lesson", null, now.plusDays(5)));
    doReturn(now.plusDays(3).toInstant(ZoneOffset.UTC)).when(clock).instant();

    assertEquals(400, rescheduleStatus(authorization, id,
        jsonMapper.writeValueAsString(Map.of("appointmentDate", now.plusDays(2)))));
  }

  @Test
  void overlappingCreatesAreRejectedButAdjacentAndCancelledSlotsCanBeUsed() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    long firstId = createAppointment(authorization,
        new AppointmentRequest("First lesson", null, start, null, 90));

    AppointmentRequest overlapping =
        new AppointmentRequest("Second lesson", null, start.plusMinutes(30), null, 30);
    assertEquals(409, createStatus(authorization, overlapping));

    createAppointment(authorization,
        new AppointmentRequest("Adjacent lesson", null, start.plusMinutes(90), null, 30));
    cancelAppointment(authorization, firstId);
    assertEquals(201, createStatus(authorization, overlapping));
  }

  @Test
  void reschedulingOrIncreasingDurationCannotOverlapAnotherAppointment() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    long firstId = createAppointment(authorization,
        new AppointmentRequest("First lesson", null, start, null, 30));
    long secondId = createAppointment(authorization,
        new AppointmentRequest("Second lesson", null, start.plusMinutes(45), null, 30));

    assertEquals(409, rescheduleStatus(authorization, secondId,
        jsonMapper.writeValueAsString(Map.of("appointmentDate", start.plusMinutes(15)))));
    assertEquals(409, rescheduleStatus(authorization, firstId,
        jsonMapper.writeValueAsString(Map.of("durationMinutes", 60))));

    mockMvc.perform(get("/appointments/{id}", firstId)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").value(30));
    assertEquals(1, appointmentsAt(ownerEmail, start.plusMinutes(45)));
  }

  @Test
  void concurrentWritesWithDifferentStartsCannotReserveOverlappingIntervals() throws Exception {
    String authorization = registerAndLogin("Appointment Owner", ownerEmail);
    LocalDateTime start = LocalDateTime.now(ZoneOffset.UTC).plusDays(4).withNano(0);
    AppointmentRequest first = new AppointmentRequest("First lesson", null, start, null, 60);
    AppointmentRequest second =
        new AppointmentRequest("Second lesson", null, start.plusMinutes(30), null, 60);

    assertEquals(List.of(201, 409), runCompetingWrites(
        () -> createStatus(authorization, first), () -> createStatus(authorization, second)));
    assertEquals(1, appointmentsAt(ownerEmail, start)
        + appointmentsAt(ownerEmail, start.plusMinutes(30)));
  }

  @Test
  void intervalConstraintUpgradeReportsLegacyOverlapsWithoutDeletingRecords() throws Exception {
    String script = new ClassPathResource("appointment-slot-constraints.sql")
        .getContentAsString(StandardCharsets.UTF_8);
    String sql = script.substring(0, script.lastIndexOf("^^^"));
    try (var connection = jdbcTemplate.getDataSource().getConnection();
         var statement = connection.createStatement()) {
      connection.setAutoCommit(false);
      try {
        statement.execute("""
            create temporary table appointments (
              id bigint primary key,
              user_id bigint not null,
              appointment_date timestamp not null,
              status varchar not null,
              duration_minutes integer
            )
            """);
        statement.execute("""
            create unique index ux_appointments_user_active_slot on appointments
                (user_id, appointment_date) where status <> 'CANCELLED'
            """);
        statement.execute("""
            insert into appointments values
                (1, 42, '2030-01-07 10:00:00', 'CONFIRMED', null),
                (2, 42, '2030-01-07 10:15:00', 'PENDING', null)
            """);
        var beforeUpgrade = connection.setSavepoint();

        SQLException conflict = assertThrows(SQLException.class, () -> statement.execute(sql));
        assertEquals("23P01", conflict.getSQLState());
        connection.rollback(beforeUpgrade);

        try (var rows = statement.executeQuery("""
            select count(*), count(duration_minutes),
                to_regclass('pg_temp.ux_appointments_user_active_slot') is not null
            from appointments
            """)) {
          assertTrue(rows.next());
          assertEquals(2, rows.getInt(1));
          assertEquals(0, rows.getInt(2));
          assertTrue(rows.getBoolean(3));
        }

        statement.execute("update appointments set status = 'CANCELLED' where id = 2");
        statement.execute(sql);
        statement.execute(sql);
        try (var rows = statement.executeQuery("select count(*) from appointments")) {
          assertTrue(rows.next());
          assertEquals(2, rows.getInt(1));
        }
      } finally {
        connection.rollback();
      }
    }
  }

  private int createStatus(String authorization, AppointmentRequest request) throws Exception {
    return mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(request)))
        .andReturn().getResponse().getStatus();
  }

  private void cancelAppointment(String authorization, long id) throws Exception {
    mockMvc.perform(patch("/appointments/{id}/cancel", id)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk());
  }

  private int rescheduleStatus(String authorization, long id, String request) throws Exception {
    return mockMvc.perform(patch("/appointments/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(request))
        .andReturn().getResponse().getStatus();
  }

  private int appointmentsAt(String email, LocalDateTime date) {
    return jdbcTemplate.queryForObject("""
        select count(*) from appointments a join users u on a.user_id = u.id
        where u.email = ? and a.appointment_date = ? and a.status <> 'CANCELLED'
        """, Integer.class, email, date);
  }

  private List<Integer> runCompetingWrites(Callable<Integer> first, Callable<Integer> second)
      throws Exception {
    try (var executor = Executors.newFixedThreadPool(2)) {
      var writes = new TransactionTemplate(transactionManager).execute(transaction -> {
        // Hold the first write so the other request also reaches the lock chain.
        jdbcTemplate.execute("lock table appointments in share mode");
        var requests = List.of(executor.submit(first), executor.submit(second));
        awaitBlockedAppointmentWrites();
        return requests;
      });
      return List.of(writes.get(0).get(10, TimeUnit.SECONDS),
              writes.get(1).get(10, TimeUnit.SECONDS))
          .stream().sorted().toList();
    }
  }

  private void awaitBlockedAppointmentWrites() {
    long deadline = System.nanoTime() + Duration.ofSeconds(10).toNanos();
    while (System.nanoTime() < deadline) {
      jdbcTemplate.execute("select pg_stat_clear_snapshot()");
      int blocked = jdbcTemplate.queryForObject("""
          with recursive blocked(pid) as (
            select pg_backend_pid()
            union
            select a.pid from pg_stat_activity a join blocked b
              on b.pid = any(pg_blocking_pids(a.pid))
            where a.datname = current_database()
          )
          select count(*) - 1 from blocked
          """, Integer.class);
      if (blocked == 2) {
        return;
      }
      try {
        Thread.sleep(25);
      } catch (InterruptedException exception) {
        Thread.currentThread().interrupt();
        throw new AssertionError("Interrupted while waiting for both writes", exception);
      }
    }
    throw new AssertionError("Both requests must wait in the database before releasing the lock");
  }

  private List<LocalDateTime> scheduledTimes(long appointmentId) {
    return jdbcTemplate.query("""
        select scheduled_at from appointment_reminder_schedule
        where appointment_id = ? order by scheduled_at
        """, (row, index) -> row.getObject("scheduled_at", LocalDateTime.class), appointmentId);
  }

  private long createAppointment(String ownerAuthorization) throws Exception {
    return createAppointment(ownerAuthorization, new AppointmentRequest(
        "Math lesson", "Review algebra", LocalDateTime.now(ZoneOffset.UTC).plusDays(2).withNano(0)));
  }

  private long createAppointment(String ownerAuthorization, AppointmentRequest request)
      throws Exception {
    String response = mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(request)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.status").value("PENDING"))
        .andReturn().getResponse().getContentAsString();
    return jsonMapper.readValue(response, AppointmentResponse.class).id();
  }

  private String registerAndLogin(String name, String email) throws Exception {
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new RegisterRequest(name, email, PASSWORD))))
        .andExpect(status().isCreated());

    String response = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();

    return "Bearer " + jsonMapper.readValue(response, LoginResponse.class).accessToken();
  }
}
