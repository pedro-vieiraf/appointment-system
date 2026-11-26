package com.pedrovieira.appointmentsystem;

import static org.mockito.Mockito.doReturn;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.dto.response.AppointmentResponse;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import jakarta.persistence.EntityManager;
import java.time.Clock;
import java.time.Instant;
import java.util.UUID;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoSpyBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;
import tools.jackson.databind.json.JsonMapper;

@SpringBootTest(properties = "jwt.expiration=15m")
@AutoConfigureMockMvc
@Transactional
class WeeklyAvailabilityIntegrationTest {

  private static final String PATH = "/users/me/availability";

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @Autowired
  private EntityManager entityManager;

  @MockitoSpyBean
  private Clock clock;

  @Test
  void ownerCanConfigureSplitHoursAndPreviewSlotsInUtc() throws Exception {
    doReturn(Instant.parse("2030-01-07T13:00:00Z")).when(clock).instant();
    mockMvc.perform(get(PATH)).andExpect(status().isForbidden());
    String ownerAuthorization = registerAndLogin();
    String otherAuthorization = registerAndLogin();

    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "timeZone": "America/Sao_Paulo",
                  "durationMinutes": 45,
                  "windows": [
                    {"dayOfWeek":"MONDAY","start":"10:00","end":"11:30"},
                    {"dayOfWeek":"MONDAY","start":"13:00","end":"14:00"}
                  ]
                }
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").value(45));
    entityManager.flush();
    entityManager.clear();

    mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.timeZone").value("America/Sao_Paulo"))
        .andExpect(jsonPath("$.windows.length()").value(2));
    mockMvc.perform(get(PATH).header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isNotFound());

    mockMvc.perform(get(PATH + "/candidate-slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].start").value("2030-01-07T13:45:00Z"))
        .andExpect(jsonPath("$[1].end").value("2030-01-07T16:45:00Z"));

    mockMvc.perform(get(PATH + "/candidate-slots").param("date", "2030-01-08")
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(get(PATH + "/candidate-slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());

    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"timeZone":"America/Sao_Paulo","durationMinutes":45,"windows":[]}
                """))
        .andExpect(status().isOk());
    entityManager.flush();
    entityManager.clear();

    mockMvc.perform(get(PATH + "/candidate-slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$").isEmpty());
  }

  @Test
  void rejectsOverlappingWorkingHours() throws Exception {
    String authorization = registerAndLogin();

    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "timeZone": "America/Sao_Paulo",
                  "durationMinutes": 45,
                  "windows": [
                    {"dayOfWeek":"MONDAY","start":"10:00","end":"12:00"},
                    {"dayOfWeek":"MONDAY","start":"11:30","end":"14:00"}
                  ]
                }
                """))
        .andExpect(status().isBadRequest());
  }

  @Test
  void previewSkipsIntervalsWithNonexistentLocalTimes() throws Exception {
    doReturn(Instant.parse("2030-03-09T12:00:00Z")).when(clock).instant();
    String authorization = registerAndLogin();
    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "timeZone": "America/New_York",
                  "durationMinutes": 30,
                  "windows": [{"dayOfWeek":"SUNDAY","start":"01:00","end":"04:00"}]
                }
                """))
        .andExpect(status().isOk());

    mockMvc.perform(get(PATH + "/candidate-slots").param("date", "2030-03-10")
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].start").value("2030-03-10T06:00:00Z"))
        .andExpect(jsonPath("$[1].start").value("2030-03-10T07:00:00Z"))
        .andExpect(jsonPath("$[2].start").value("2030-03-10T07:30:00Z"));
  }

  @Test
  void freeSlotsExcludeOnlyOverlappingNonCancelledAppointmentsForTheOwner() throws Exception {
    doReturn(Instant.parse("2030-01-06T12:00:00Z")).when(clock).instant();
    String owner = registerAndLogin();
    String other = registerAndLogin();
    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "timeZone":"America/Sao_Paulo", "durationMinutes":30,
                  "windows":[{"dayOfWeek":"MONDAY","start":"10:00","end":"12:30"}]
                }
                """))
        .andExpect(status().isOk());

    long appointmentId = createAppointment(owner, "2030-01-07T13:15:00", 45);
    long cancelledId = createAppointment(owner, "2030-01-07T14:30:00", 30);
    createAppointment(other, "2030-01-07T14:00:00", 30);
    mockMvc.perform(patch("/appointments/{id}/cancel", cancelledId)
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk());
    entityManager.flush();
    entityManager.clear();

    mockMvc.perform(get(PATH + "/slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(3))
        .andExpect(jsonPath("$[0].start").value("2030-01-07T14:00:00Z"))
        .andExpect(jsonPath("$[1].start").value("2030-01-07T14:30:00Z"));

    mockMvc.perform(patch("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, owner)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"durationMinutes":75}
                """))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").value(75));
    entityManager.flush();
    entityManager.clear();

    mockMvc.perform(get(PATH + "/slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, owner))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(2))
        .andExpect(jsonPath("$[0].start").value("2030-01-07T14:30:00Z"));
  }

  @Test
  void legacyAppointmentStartingThePreviousDayStillBlocksAnOverlappingSlot() throws Exception {
    doReturn(Instant.parse("2030-01-06T12:00:00Z")).when(clock).instant();
    String authorization = registerAndLogin();
    mockMvc.perform(put(PATH)
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {
                  "timeZone":"UTC", "durationMinutes":30,
                  "windows":[{"dayOfWeek":"MONDAY","start":"00:00","end":"01:00"}]
                }
                """))
        .andExpect(status().isOk());
    long id = createAppointment(authorization, "2030-01-06T23:45:00", 30);
    entityManager.flush();
    entityManager.createNativeQuery("update appointments set duration_minutes = null where id = :id")
        .setParameter("id", id)
        .executeUpdate();
    entityManager.clear();

    mockMvc.perform(get("/appointments/{id}", id)
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.durationMinutes").value(30));
    mockMvc.perform(get(PATH + "/slots").param("date", "2030-01-07")
            .header(HttpHeaders.AUTHORIZATION, authorization))
        .andExpect(status().isOk())
        .andExpect(jsonPath("$.length()").value(1))
        .andExpect(jsonPath("$[0].start").value("2030-01-07T00:30:00Z"));
  }

  private long createAppointment(String authorization, String date, int duration) throws Exception {
    String response = mockMvc.perform(post("/appointments")
            .header(HttpHeaders.AUTHORIZATION, authorization)
            .contentType(MediaType.APPLICATION_JSON)
            .content("""
                {"title":"Math lesson","appointmentDate":"%s","durationMinutes":%d}
                """.formatted(date, duration)))
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.durationMinutes").value(duration))
        .andReturn().getResponse().getContentAsString();
    return jsonMapper.readValue(response, AppointmentResponse.class).id();
  }

  private String registerAndLogin() throws Exception {
    String email = UUID.randomUUID() + "@example.test";
    String password = "availability-test-password";
    mockMvc.perform(post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(
                new RegisterRequest("Availability Owner", email, password))))
        .andExpect(status().isCreated());
    String response = mockMvc.perform(post("/auth/login")
            .contentType(MediaType.APPLICATION_JSON)
            .content(jsonMapper.writeValueAsString(new LoginRequest(email, password))))
        .andExpect(status().isOk())
        .andReturn().getResponse().getContentAsString();
    return "Bearer " + jsonMapper.readValue(response, LoginResponse.class).accessToken();
  }
}
