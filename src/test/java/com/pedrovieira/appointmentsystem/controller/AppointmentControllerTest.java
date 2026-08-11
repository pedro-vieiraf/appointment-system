package com.pedrovieira.appointmentsystem.controller;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.security.JwtService;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
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
    @DisplayName("Should return bad request when title is below minimum size")
    public void shouldReturnBadRequestWhenTitleIsBelowMinimumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      AppointmentRequest request = new AppointmentRequest(
          "AB",
          "Description",
          appointmentDate
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when title exceeds maximum size")
    public void shouldReturnBadRequestWhenTitleExceedsMaximumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      String title = "A".repeat(51);

      AppointmentRequest request = new AppointmentRequest(
          title,
          "Description",
          appointmentDate
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
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
          null,
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
          null,
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
          .andExpect(jsonPath("$.description").doesNotExist())
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(appointmentDate.toString())
          );
    }

    @Test
    @DisplayName("Should return bad request when description is below minimum size")
    public void shouldReturnBadRequestWhenDescriptionIsBelowMinimumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          "AB",
          appointmentDate
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when description exceeds maximum size")
    public void shouldReturnBadRequestWhenDescriptionExceedsMaximumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      String description = "A".repeat(501);

      AppointmentRequest request = new AppointmentRequest(
          "Medical appointment",
          description,
          appointmentDate
      );

      // Act + Assert
      mockMvc.perform(
              post("/appointments")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
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

  @Nested
  @DisplayName("GET All Appointments")
  class GetAllAppointments {

    @Test
    @DisplayName("Should get appointments successfully")
    public void shouldGetAppointmentsSuccessfully() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );
      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental checkup",
          LocalDateTime.now().plusDays(2)
      );
      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      when(appointmentService.getAll(user))
          .thenReturn(appointments);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(2))
          .andExpect(jsonPath("$[0].id").value(1))
          .andExpect(jsonPath("$[0].title").value("Medical appointment"))
          .andExpect(jsonPath("$[0].description").value("Annual checkup"))
          .andExpect(jsonPath("$[1].id").value(2))
          .andExpect(jsonPath("$[1].title").value("Dentist"))
          .andExpect(jsonPath("$[1].description").value("Dental checkup"));

      verify(appointmentService).getAll(user);
    }

    @Test
    @DisplayName("Should return an empty list when user has no appointments")
    void shouldReturnEmptyListWhenUserHasNoAppointments() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      when(appointmentService.getAll(user))
          .thenReturn(List.of());

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$").isArray())
          .andExpect(jsonPath("$.length()").value(0));

      verify(appointmentService).getAll(user);
    }
  }

  @Nested
  @DisplayName("GET Appointment By ID")
  class GetAppointmentById {

    @Test
    @DisplayName("Should get the appointment by ID successfully")
    public void shouldGetTheAppointmentByIdSuccessfully() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );
      appointment.setId(1L);

      when(appointmentService.getById(user, appointment.getId()))
          .thenReturn(appointment);

      // Act + Assert
      mockMvc.perform(
              get("/appointments/{id}", appointment.getId())
                  .with(user(user))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.title").value("Medical appointment"))
          .andExpect(jsonPath("$.description").value("Annual checkup"));

      verify(appointmentService)
          .getById(user, appointment.getId());
    }

    @Test
    @DisplayName("Should return 404 when appointment is not found")
    void shouldReturnNotFoundWhenAppointmentIsNotFound() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      when(appointmentService.getById(user, appointmentId))
          .thenThrow(new AppointmentNotFoundException());

      // Act + Assert
      mockMvc.perform(
              get("/appointments/{id}", appointmentId)
                  .with(user(user))
          )
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Appointment not found."));

      verify(appointmentService)
          .getById(user, appointmentId);
    }
  }

  @Nested
  @DisplayName("PATCH Appointment")
  class PatchAppointment {

    @Test
    @DisplayName("Should update the title successfully")
    public void shouldUpdateTitleSuccessfully() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "Updated appointment",
              null,
              null
          );

      Appointment appointment = new Appointment(
          user,
          "Updated appointment",
          "Annual checkup",
          appointmentDate
      );

      appointment.setId(appointmentId);

      when(appointmentService.update(
          user,
          appointmentId,
          request
      )).thenReturn(appointment);

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(appointmentId))
          .andExpect(jsonPath("$.title").value("Updated appointment"))
          .andExpect(jsonPath("$.description").value("Annual checkup"))
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(appointmentDate.toString())
          );

      verify(appointmentService)
          .update(user, appointmentId, request);
    }

    @Test
    @DisplayName("Should update only the appointment description")
    public void shouldUpdateOnlyTheAppointmentDescription() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      LocalDateTime appointmentDate =
          LocalDateTime.now()
              .plusDays(1)
              .withNano(0);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              "Updated description",
              null
          );

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Updated description",
          appointmentDate
      );

      appointment.setId(appointmentId);

      when(appointmentService.update(
          user,
          appointmentId,
          request
      )).thenReturn(appointment);

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(appointmentId))
          .andExpect(
              jsonPath("$.title")
                  .value("Medical appointment")
          )
          .andExpect(
              jsonPath("$.description")
                  .value("Updated description")
          )
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(appointmentDate.toString())
          );

      verify(appointmentService)
          .update(user, appointmentId, request);
    }

    @Test
    @DisplayName("Should update only the appointment date")
    public void shouldUpdateOnlyTheAppointmentDate() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      LocalDateTime updatedDate =
          LocalDateTime.now()
              .plusDays(2)
              .withNano(0);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              null,
              updatedDate
          );

      Appointment appointment = new Appointment(
          user,
          "Medical appointment",
          "Annual checkup",
          updatedDate
      );

      appointment.setId(appointmentId);

      when(appointmentService.update(
          user,
          appointmentId,
          request
      )).thenReturn(appointment);

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(appointmentId))
          .andExpect(
              jsonPath("$.title")
                  .value("Medical appointment")
          )
          .andExpect(
              jsonPath("$.description")
                  .value("Annual checkup")
          )
          .andExpect(
              jsonPath("$.appointmentDate")
                  .value(updatedDate.toString())
          );

      verify(appointmentService)
          .update(user, appointmentId, request);
    }

    @Test
    @DisplayName("Should return bad request when title is below minimum size")
    public void shouldReturnBadRequestWhenTitleIsBelowMinimumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              "AB",
              null,
              null
          );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when title exceeds maximum size")
    public void shouldReturnBadRequestWhenTitleExceedsMaximumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      String title = "A".repeat(51);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              title,
              null,
              null
          );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when description is below minimum size")
    public void shouldReturnBadRequestWhenDescriptionIsBelowMinimumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              "AB",
              null
          );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when description exceeds maximum size")
    public void shouldReturnBadRequestWhenDescriptionExceedsMaximumSize()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      String description = "A".repeat(501);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              description,
              null
          );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return bad request when appointment date is in the past")
    public void shouldReturnBadRequestWhenAppointmentDateIsInThePast()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      LocalDateTime pastDate =
          LocalDateTime.now()
              .minusDays(1)
              .withNano(0);

      UpdateAppointmentRequest request =
          new UpdateAppointmentRequest(
              null,
              null,
              pastDate
          );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return 404 when appointment is not found")
    public void shouldReturn404WhenAppointmentIsNotFound() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request = new UpdateAppointmentRequest(
          "Updated title",
          null,
          null
      );

      when(appointmentService.update(
          eq(user),
          eq(appointmentId),
          any(UpdateAppointmentRequest.class)
      )).thenThrow(new AppointmentNotFoundException()
      );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Appointment not found."));

      verify(appointmentService).update(
          eq(user),
          eq(appointmentId),
          any(UpdateAppointmentRequest.class)
      );
    }

    @Test
    @DisplayName("Should return 404 when appointment does not belong to user")
    public void shouldReturn404WhenAppointmentDoesNotBelongToUser()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      UpdateAppointmentRequest request = new UpdateAppointmentRequest(
          "Updated title",
          null,
          null
      );

      when(appointmentService.update(
          eq(user),
          eq(appointmentId),
          any(UpdateAppointmentRequest.class)
      )).thenThrow(
          new AppointmentNotFoundException()
      );

      // Act + Assert
      mockMvc.perform(
              patch("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isNotFound())
          .andExpect(jsonPath("$.message").value("Appointment not found."));

      verify(appointmentService).update(
          eq(user),
          eq(appointmentId),
          any(UpdateAppointmentRequest.class)
      );
    }
  }
}
