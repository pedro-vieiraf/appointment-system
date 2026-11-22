package com.pedrovieira.appointmentsystem;

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
import java.time.LocalDateTime;
import java.time.ZoneOffset;
import java.util.UUID;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.web.servlet.MockMvc;
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

  @AfterEach
  void deleteTestRecords() {
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

    mockMvc.perform(delete("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, otherAuthorization))
        .andExpect(status().isNotFound());

    mockMvc.perform(delete("/appointments/{id}", appointmentId)
            .header(HttpHeaders.AUTHORIZATION, ownerAuthorization))
        .andExpect(status().isNoContent());

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

  private long createAppointment(String ownerAuthorization) throws Exception {
    var request = new AppointmentRequest("Math lesson", "Review algebra",
        LocalDateTime.now(ZoneOffset.UTC).plusDays(2).withNano(0));
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
