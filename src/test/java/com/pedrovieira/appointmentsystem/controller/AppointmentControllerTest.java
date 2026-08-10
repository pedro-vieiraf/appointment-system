package com.pedrovieira.appointmentsystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(AppointmentController.class)
@DisplayName("Appointment Controller Tests")
public class AppointmentControllerTest {

  @Autowired
  private MockMvc mockMvc;

  @Autowired
  private JsonMapper jsonMapper;

  @MockitoBean
  private AppointmentService appointmentService;

  @Nested
  @DisplayName("POST Appointment")
  class PostAppointment {

    @Test
    @DisplayName("Should create new appointment")
    public void shouldCreateNewAppointment() throws Exception {
      // Arrange
      LocalDateTime appointmentDate = LocalDateTime.now().plusDays(1);

      AppointmentRequest request = new AppointmentRequest(
          "Test",
          "Description",
          appointmentDate
      );

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Test",
          "Description",
          appointmentDate
      );

      when(appointmentService.create(
          any(User.class),
          any(AppointmentRequest.class)
      )).thenReturn(appointment);

      // Act
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Test"))
          .andExpect(jsonPath("$.description").value("Description"))
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(appointmentDate.toString())
          );
    }
  }
}
