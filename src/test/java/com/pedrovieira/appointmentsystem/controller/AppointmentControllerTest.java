package com.pedrovieira.appointmentsystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.security.JwtService;
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

  @MockitoBean
  private JwtService jwtService;

  @MockitoBean
  private UserRepository userRepository;

  @Nested
  @DisplayName("POST Appointment")
  class PostAppointment {

    @Test
    @DisplayName("Should create new appointment")
    public void shouldCreateNewAppointment() throws Exception {
      // Arrange
      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

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
                  .with(csrf())
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

    @Test
    @DisplayName("Should create new appointment without description")
    public void shouldCreateNewAppointmentWithoutDescription() throws Exception {
      // Arrange
      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      AppointmentRequest request = new AppointmentRequest(
          "Test",
          "",
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
          "",
          appointmentDate
      );

      when(appointmentService.create(
          any(User.class),
          any(AppointmentRequest.class)
      )).thenReturn(appointment);

      // Act
      mockMvc.perform(
              post("/appointments")
                  .with(csrf())
                  .with(user(user))
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isCreated())
          .andExpect(jsonPath("$.title").value("Test"))
          .andExpect(jsonPath("$.description").value(""))
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(appointmentDate.toString())
          );
    }

    @Test
    @DisplayName("Should return error if no title in body")
    void shouldReturnErrorIfNoTitleInBody() throws Exception {
      // Arrange
      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      AppointmentRequest request = new AppointmentRequest(
          "",
          "Description",
          appointmentDate
      );

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("Title is required.")
          );

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return error if appointment date is missing")
    void shouldReturnErrorIfAppointmentDateIsMissing() throws Exception {
      // Arrange
      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          "Annual checkup",
          null
      );

      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("Appointment date is required.")
          );

      verifyNoInteractions(appointmentService);
    }
  }

  @Test
  @DisplayName("Should return error if appointment date is in the past")
  void shouldReturnErrorIfAppointmentDateIsInThePast() throws Exception {
    // Arrange
    AppointmentRequest request = new AppointmentRequest(
        "Medical appointment",
        "Annual checkup",
        LocalDateTime.now().minusDays(1)
    );

    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    // Act + Assert
    mockMvc.perform(
            post("/appointments")
                .with(user(user))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(
            jsonPath("$.message")
                .value("Appointment date must be in the future.")
        );

    verifyNoInteractions(appointmentService);
  }

  @Test
  @DisplayName("Should create appointment for authenticated user")
  void shouldCreateAppointmentForAuthenticatedUser() throws Exception {
    // Arrange
    LocalDateTime appointmentDate =
        LocalDateTime.now()
            .plusDays(1)
            .withNano(0);

    AppointmentRequest request = new AppointmentRequest(
        "Medical appointment",
        "Annual checkup",
        appointmentDate
    );

    User user = new User(
        "John Doe",
        "john@email.com",
        "hashed-password"
    );

    Appointment appointment = new Appointment(
        user,
        "Medical appointment",
        "Annual checkup",
        appointmentDate
    );

    when(
        appointmentService.create(
            any(User.class),
            any(AppointmentRequest.class)
        )
    ).thenReturn(appointment);

    // Act
    mockMvc.perform(
            post("/appointments")
                .with(user(user))
                .with(csrf())
                .contentType(MediaType.APPLICATION_JSON)
                .content(jsonMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated());

    // Assert
    verify(appointmentService).create(
        eq(user),
        eq(request)
    );
  }
}
