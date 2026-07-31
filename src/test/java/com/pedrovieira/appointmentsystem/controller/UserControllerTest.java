package com.pedrovieira.appointmentsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedrovieira.appointmentsystem.config.SecurityConfig;
import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.user.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.service.UserService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.context.bean.override.mockito.MockitoBean;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(UserController.class)
@Import(SecurityConfig.class)
@DisplayName("Testing UserController functions")
public class UserControllerTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private UserService userService;

  @Test
  public void shouldCreateUserSuccessfully() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    User user =
        new User(
            request.fullName(),
            request.email(),
            "encrypted"
        );

    when(userService.createUser(any()))
        .thenReturn(user);

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fullName").value("User Resu"))
        .andExpect(jsonPath("$.email").value("user@email.com"));

    ArgumentCaptor<CreateUserRequest> captor =
        ArgumentCaptor.forClass(CreateUserRequest.class);

    verify(userService).createUser(captor.capture());

    CreateUserRequest sent = captor.getValue();

    assertEquals("User Resu", sent.fullName());
    assertEquals("user@email.com", sent.email());
    assertEquals("superstrongpassword", sent.password());
  }

  @Test
  public void shouldReturnErrorIfNameIsTooShort() throws Exception {
    CreateUserRequest request = new CreateUserRequest(
        "A",
        "user@email.com",
        "12345678"
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Full name must contain between 2 and 100 characters."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfNameIsTooLong() throws Exception {
    String longName = "A".repeat(51);

    CreateUserRequest request = new CreateUserRequest(
        longName,
        "user@email.com",
        "12345678"
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message")
            .value("Full name must contain between 2 and 100 characters."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfBlankEmail() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "",
        "superstrongpassword"
    );

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Email is required."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfEmailAlreadyExists() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userService.createUser(any()))
        .thenThrow(new EmailAlreadyExistsException());

    // Act + Assert
    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isConflict())
        .andExpect(jsonPath("$.status").value(409))
        .andExpect(jsonPath("$.error").value("Conflict"))
        .andExpect(jsonPath("$.message")
            .value("Email already registered."))
        .andExpect(jsonPath("$.path").value("/auth/register"));
  }

  @Test
  public void shouldReturnErrorIfInvalidEmail() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "invalidEmail@.com",
        "superstrongpassword"
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Email must be valid."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfBlankName() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "",
        "user@email.com",
        "superstrongpassword"
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Full name is required."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfBlankPassword() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        ""
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Password must have at least 8 characters."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnErrorIfShortPassword() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "1234567"
    );

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Password must have at least 8 characters."));

    verifyNoInteractions(userService);
  }

  @Test
  public void shouldReturnBadRequestWhenJsonIsInvalid() throws Exception {
    // Arrange
    String invalidJson = """
      {
        "fullName": "User Resu",
        "email": "user@email.com",
        "password": "123456"
      """;

    // Act + Assert
    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(invalidJson)
        )
        .andExpect(status().isBadRequest())
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Malformed JSON request."));

    verifyNoInteractions(userService);
  }
}
