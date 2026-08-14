package com.pedrovieira.appointmentsystem.controller;

import static org.hamcrest.Matchers.hasSize;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentRequest;
import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.dto.request.UpdateAppointmentRequest;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.appointments.AppointmentNotFoundException;
import com.pedrovieira.appointmentsystem.exception.appointments.InvalidAppointmentDateRangeException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.security.JwtService;
import com.pedrovieira.appointmentsystem.service.AppointmentService;
import java.time.LocalDateTime;
import java.util.List;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
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
                  .value(
                      org.hamcrest.Matchers.startsWith(
                          appointmentDate.toString()
                      )
                  )
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
    void shouldGetAppointmentsSuccessfully() throws Exception {
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

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(
              0,
              10,
              Sort.by(
                  Sort.Direction.ASC,
                  "appointmentDate"
              )
          );

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(2))
          .andExpect(jsonPath("$.content[0].id").value(1))
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Medical appointment")
          )
          .andExpect(
              jsonPath("$.content[0].description")
                  .value("Annual checkup")
          )
          .andExpect(jsonPath("$.content[1].id").value(2))
          .andExpect(
              jsonPath("$.content[1].title")
                  .value("Dentist")
          )
          .andExpect(
              jsonPath("$.content[1].description")
                  .value("Dental checkup")
          )
          .andExpect(jsonPath("$.totalElements").value(2))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should return an empty page when user has no appointments")
    void shouldReturnEmptyPageWhenUserHasNoAppointments() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> emptyPage =
          new PageImpl<>(
              List.of(),
              pageable,
              0
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(emptyPage);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(0))
          .andExpect(jsonPath("$.totalElements").value(0))
          .andExpect(jsonPath("$.totalPages").value(0))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should return paginated appointments")
    void shouldReturnPaginatedAppointments() throws Exception {
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
          "Dental cleaning",
          LocalDateTime.now().plusDays(2)
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(1, 2);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              4
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param("page", "1")
                  .param("size", "2")
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content", hasSize(2)))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(1)
          )
          .andExpect(
              jsonPath("$.content[1].id")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.totalElements")
                  .value(4)
          )
          .andExpect(
              jsonPath("$.size")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.number")
                  .value(1)
          );

      verify(appointmentService).getAll(
          eq(user),
          eq(criteria),
          any(Pageable.class)
      );
    }

    @Test
    @DisplayName("Should sort appointments by date descending")
    void shouldSortAppointmentsByDateDescending() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental cleaning",
          LocalDateTime.now().plusDays(2)
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment2,
          appointment1
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(
              0,
              10,
              Sort.by(
                  Sort.Direction.DESC,
                  "appointmentDate"
              )
          );

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "sort",
                      "appointmentDate,desc"
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content", hasSize(2)))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.content[1].id")
                  .value(1)
          );

      ArgumentCaptor<Pageable> pageableCaptor =
          ArgumentCaptor.forClass(Pageable.class);

      verify(appointmentService).getAll(
          eq(user),
          eq(criteria),
          pageableCaptor.capture()
      );

      Pageable capturedPageable =
          pageableCaptor.getValue();

      assertEquals(0, capturedPageable.getPageNumber());
      assertEquals(10, capturedPageable.getPageSize());

      Sort.Order order =
          capturedPageable.getSort()
              .getOrderFor("appointmentDate");

      assertNotNull(order);
      assertEquals(
          Sort.Direction.DESC,
          order.getDirection()
      );
    }

    @Test
    @DisplayName("Should sort appointments by date ascending")
    void shouldSortAppointmentsByDateAscending() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental cleaning",
          LocalDateTime.now().plusDays(2)
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(
              0,
              10,
              Sort.by(
                  Sort.Direction.ASC,
                  "appointmentDate"
              )
          );

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "sort",
                      "appointmentDate,asc"
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content", hasSize(2)))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(1)
          )
          .andExpect(
              jsonPath("$.content[1].id")
                  .value(2)
          );

      ArgumentCaptor<Pageable> pageableCaptor =
          ArgumentCaptor.forClass(Pageable.class);

      verify(appointmentService).getAll(
          eq(user),
          eq(criteria),
          pageableCaptor.capture()
      );

      Pageable capturedPageable =
          pageableCaptor.getValue();

      assertEquals(0, capturedPageable.getPageNumber());
      assertEquals(10, capturedPageable.getPageSize());

      Sort.Order order =
          capturedPageable.getSort()
              .getOrderFor("appointmentDate");

      assertNotNull(order);
      assertEquals(
          Sort.Direction.ASC,
          order.getDirection()
      );
    }

    @Test
    @DisplayName("Should apply pagination and sorting")
    void shouldApplyPaginationAndSorting() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(3)
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental cleaning",
          LocalDateTime.now().plusDays(2)
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1,
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(
              1,
              2,
              Sort.by(
                  Sort.Direction.DESC,
                  "appointmentDate"
              )
          );

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              4
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param("page", "1")
                  .param("size", "2")
                  .param(
                      "sort",
                      "appointmentDate,desc"
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content", hasSize(2)))
          .andExpect(
              jsonPath("$.totalElements")
                  .value(4)
          )
          .andExpect(
              jsonPath("$.size")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.number")
                  .value(1)
          );

      ArgumentCaptor<Pageable> pageableCaptor =
          ArgumentCaptor.forClass(Pageable.class);

      verify(appointmentService).getAll(
          eq(user),
          eq(criteria),
          pageableCaptor.capture()
      );

      Pageable capturedPageable =
          pageableCaptor.getValue();

      assertEquals(1, capturedPageable.getPageNumber());
      assertEquals(2, capturedPageable.getPageSize());

      Sort.Order order =
          capturedPageable.getSort()
              .getOrderFor("appointmentDate");

      assertNotNull(order);
      assertEquals(
          Sort.Direction.DESC,
          order.getDirection()
      );
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
  @DisplayName("SEARCH User")
  class SearchUser {

    @Test
    @DisplayName("Should filter appointments by title")
    void shouldFilterAppointmentsByTitle() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          LocalDateTime.now().plusDays(1)
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          LocalDateTime.now().plusDays(2)
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              null,
              null
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param("title", "Doctor")
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Doctor")
          )
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should filter appointments by start date")
    void shouldFilterAppointmentsByStartDate() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now = LocalDateTime.now();

      LocalDateTime startDate =
          now.plusDays(2);

      LocalDateTime appointmentDate1 =
          now.plusDays(1);

      LocalDateTime appointmentDate2 =
          now.plusDays(3);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          appointmentDate1
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          appointmentDate2
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              null
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "startDate",
                      startDate.toString()
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Dentist")
          )
          .andExpect(
              jsonPath("$.content[0].description")
                  .value("Dental appointment")
          )
          .andExpect(
              jsonPath("$.content[0].appointmentDate")
                  .value(
                      org.hamcrest.Matchers.startsWith(
                          appointmentDate2
                              .withNano(0)
                              .toString()
                      )
                  )
          )
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should filter appointments by end date")
    void shouldFilterAppointmentsByEndDate() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now =
          LocalDateTime.now().withNano(0);

      LocalDateTime endDate =
          now.plusDays(4);

      LocalDateTime appointmentDate1 =
          now.plusDays(3);

      LocalDateTime appointmentDate2 =
          now.plusDays(5);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          appointmentDate1
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Dental appointment",
          appointmentDate2
      );

      appointment2.setId(2L);

      List<Appointment> appointments = List.of(
          appointment1
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              endDate
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "endDate",
                      endDate.toString()
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(1)
          )
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Doctor")
          )
          .andExpect(
              jsonPath("$.content[0].description")
                  .value("Annual checkup")
          )
          .andExpect(
              jsonPath("$.content[0].appointmentDate")
                  .value(
                      org.hamcrest.Matchers.startsWith(
                          appointmentDate1.toString()
                      )
                  )
          )
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should filter appointments by date range")
    void shouldFilterAppointmentsByDateRange() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now =
          LocalDateTime.now().withNano(0);

      LocalDateTime startDate =
          now.plusDays(2);

      LocalDateTime endDate =
          now.plusDays(5);

      LocalDateTime appointmentDate1 =
          now.plusDays(1);

      LocalDateTime appointmentDate2 =
          now.plusDays(3);

      LocalDateTime appointmentDate3 =
          now.plusDays(6);

      Appointment appointment1 = new Appointment(
          user,
          "Doctor",
          "Before range",
          appointmentDate1
      );

      appointment1.setId(1L);

      Appointment appointment2 = new Appointment(
          user,
          "Dentist",
          "Inside range",
          appointmentDate2
      );

      appointment2.setId(2L);

      Appointment appointment3 = new Appointment(
          user,
          "Cardiologist",
          "After range",
          appointmentDate3
      );

      appointment3.setId(3L);

      List<Appointment> appointments = List.of(
          appointment2
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              endDate
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "startDate",
                      startDate.toString()
                  )
                  .param(
                      "endDate",
                      endDate.toString()
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(2)
          )
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Dentist")
          )
          .andExpect(
              jsonPath("$.content[0].description")
                  .value("Inside range")
          )
          .andExpect(
              jsonPath("$.content[0].appointmentDate")
                  .value(
                      org.hamcrest.Matchers.startsWith(
                          appointmentDate2.toString()
                      )
                  )
          )
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should filter appointments by title and date range")
    void shouldFilterAppointmentsByTitleAndDateRange() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now =
          LocalDateTime.now().withNano(0);

      LocalDateTime startDate =
          now.plusDays(2);

      LocalDateTime endDate =
          now.plusDays(5);

      LocalDateTime appointmentDate =
          now.plusDays(3);

      Appointment appointment = new Appointment(
          user,
          "Doctor",
          "Annual checkup",
          appointmentDate
      );

      appointment.setId(1L);

      List<Appointment> appointments = List.of(
          appointment
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              startDate,
              endDate
          );

      Pageable pageable =
          PageRequest.of(0, 10);

      Page<Appointment> page =
          new PageImpl<>(
              appointments,
              pageable,
              appointments.size()
          );

      when(
          appointmentService.getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          )
      ).thenReturn(page);

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param("title", "Doctor")
                  .param(
                      "startDate",
                      startDate.toString()
                  )
                  .param(
                      "endDate",
                      endDate.toString()
                  )
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.content").isArray())
          .andExpect(jsonPath("$.content.length()").value(1))
          .andExpect(
              jsonPath("$.content[0].id")
                  .value(1)
          )
          .andExpect(
              jsonPath("$.content[0].title")
                  .value("Doctor")
          )
          .andExpect(
              jsonPath("$.content[0].description")
                  .value("Annual checkup")
          )
          .andExpect(
              jsonPath("$.content[0].appointmentDate")
                  .value(
                      org.hamcrest.Matchers.startsWith(
                          appointmentDate.toString()
                      )
                  )
          )
          .andExpect(jsonPath("$.totalElements").value(1))
          .andExpect(jsonPath("$.totalPages").value(1))
          .andExpect(jsonPath("$.size").value(10))
          .andExpect(jsonPath("$.number").value(0));

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
    }

    @Test
    @DisplayName("Should return 400 when start date is invalid")
    void shouldReturn400WhenStartDateIsInvalid() throws Exception {
      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(
                      new User(
                          "John Doe",
                          "john@email.com",
                          "hashed-password"
                      )
                  ))
                  .param("startDate", "invalid-date")
          )
          .andExpect(status().isBadRequest());

      verifyNoInteractions(appointmentService);
    }

    @Test
    @DisplayName("Should return 400 when end date is before start date")
    void shouldReturn400WhenEndDateIsBeforeStartDate() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime now =
          LocalDateTime.now().withNano(0);

      LocalDateTime startDate =
          now.plusDays(5);

      LocalDateTime endDate =
          now.plusDays(2);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              endDate
          );

      doThrow(new InvalidAppointmentDateRangeException())
          .when(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );

      // Act + Assert
      mockMvc.perform(
              get("/appointments")
                  .with(user(user))
                  .param(
                      "startDate",
                      startDate.toString()
                  )
                  .param(
                      "endDate",
                      endDate.toString()
                  )
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("End date must be after start date.")
          );

      verify(appointmentService)
          .getAll(
              eq(user),
              eq(criteria),
              any(Pageable.class)
          );
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

  @Nested
  @DisplayName("DELETE Appointment")
  class DeleteAppointment {

    @Test
    @DisplayName("Should delete appointment successfully")
    public void shouldDeleteAppointmentSuccessfully() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      doNothing().when(appointmentService)
          .delete(user, appointmentId);

      // Act + Assert
      mockMvc.perform(
              delete("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
          )
          .andExpect(status().isNoContent());

      verify(appointmentService)
          .delete(user, appointmentId);
    }

    @Test
    @DisplayName("Should return 404 if appointment is not found")
    public void shouldReturn404IfAppointmentIsNotFound() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      doThrow(new AppointmentNotFoundException())
          .when(appointmentService)
          .delete(user, appointmentId);

      // Act + Assert
      mockMvc.perform(
              delete("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
          )
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.message")
                  .value("Appointment not found.")
          );

      verify(appointmentService)
          .delete(user, appointmentId);
    }

    @Test
    @DisplayName("Should return 404 if appointment does not belong to user")
    public void shouldReturn404IfAppointmentDoesNotBelongToUser()
        throws Exception {

      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      Long appointmentId = 1L;

      doThrow(new AppointmentNotFoundException())
          .when(appointmentService)
          .delete(user, appointmentId);

      // Act + Assert
      mockMvc.perform(
              delete("/appointments/{id}", appointmentId)
                  .with(user(user))
                  .with(csrf())
          )
          .andExpect(status().isNotFound())
          .andExpect(
              jsonPath("$.message")
                  .value("Appointment not found.")
          );

      verify(appointmentService)
          .delete(user, appointmentId);
    }
  }
}
