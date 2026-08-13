package com.pedrovieira.appointmentsystem.controller;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.security.JwtAuthenticationFilter;
import com.pedrovieira.appointmentsystem.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

@WebMvcTest(AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
@DisplayName("Testing Register Controller Functions")
public class AuthControllerRegisterTest {

  @Autowired
  private MockMvc mockMvc;

  private final ObjectMapper objectMapper = new ObjectMapper();

  @MockitoBean
  private AuthenticationService authenticationService;

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Test
  public void shouldCreateUserSuccessfully() throws Exception {
    RegisterRequest request = new RegisterRequest(
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

    when(authenticationService.register(any()))
        .thenReturn(user);

    mockMvc.perform(
            post("/auth/register")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isCreated())
        .andExpect(jsonPath("$.fullName").value("User Resu"))
        .andExpect(jsonPath("$.email").value("user@email.com"));

    ArgumentCaptor<RegisterRequest> captor =
        ArgumentCaptor.forClass(RegisterRequest.class);

    verify(authenticationService).register(captor.capture());

    RegisterRequest sent = captor.getValue();

    assertEquals("User Resu", sent.fullName());
    assertEquals("user@email.com", sent.email());
    assertEquals("superstrongpassword", sent.password());
  }

  @Test
  public void shouldReturnErrorIfNameIsTooShort() throws Exception {
    RegisterRequest request = new RegisterRequest(
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
            .value("Full name must contain between 3 and 50 characters."));

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfNameIsTooLong() throws Exception {
    String longName = "A".repeat(51);

    RegisterRequest request = new RegisterRequest(
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
            .value("Full name must contain between 3 and 50 characters."));

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfBlankEmail() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
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

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfEmailAlreadyExists() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(authenticationService.register(any()))
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
    RegisterRequest request = new RegisterRequest(
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

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfBlankName() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
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

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfBlankPassword() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
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
        .andExpect(jsonPath("$.message").value("Password is required."));

    verifyNoInteractions(authenticationService);
  }

  @Test
  public void shouldReturnErrorIfShortPassword() throws Exception {
    // Arrange
    RegisterRequest request = new RegisterRequest(
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

    verifyNoInteractions(authenticationService);
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

    verifyNoInteractions(authenticationService);
  }
}
