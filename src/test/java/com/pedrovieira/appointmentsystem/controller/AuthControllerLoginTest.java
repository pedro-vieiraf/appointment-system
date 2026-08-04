package com.pedrovieira.appointmentsystem.controller;

import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.config.SecurityConfig;
import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import com.pedrovieira.appointmentsystem.exception.user.InvalidCredentialsException;
import com.pedrovieira.appointmentsystem.service.AuthenticationService;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.context.annotation.Import;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import tools.jackson.databind.ObjectMapper;

@WebMvcTest(AuthController.class)
@Import(SecurityConfig.class)
@DisplayName("Testing Login Controller Functions")
public class AuthControllerLoginTest {

  @Autowired
  private MockMvc mockMvc;

  @MockitoBean
  private AuthenticationService authenticationService;
  @Autowired
  private ObjectMapper objectMapper;

  @Nested
  @DisplayName("Happy Path")
  class HappyPath {

    @Test
    public void shouldReturnTokenForValidCredentials() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "userTest@mail.com",
          "superStrongPassword"
      );
      LoginResponse response = new LoginResponse(
          "fake.jwt.token",
          "Bearer",
          900
      );

      when(authenticationService.login(request))
          .thenReturn(response);

      // Act + Assert
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content(objectMapper.writeValueAsString(request)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.accessToken").value("fake.jwt.token"))
          .andExpect(jsonPath("$.tokenType").value("Bearer"))
          .andExpect(jsonPath("$.expiresIn").value(900));
    }
  }

  @Nested
  @DisplayName("Validation Errors")
  class ValidationErrors {

    @Test
    public void shouldThrowExceptionWhenInvalidEmail() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "wrongUser@mail.com",
          "superStrongPassword"
      );

      when(authenticationService.login(request))
          .thenThrow(new InvalidCredentialsException("Invalid credentials."));
      // Act + Assert
      mockMvc.perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message")
              .value("Invalid credentials."));
    }

    @Test
    public void shouldThrowExceptionWhenInvalidPassword() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "userTest@mail.com",
          "wrongPassword"
      );

      when(authenticationService.login(request))
          .thenThrow(new InvalidCredentialsException("Invalid credentials."));

      // Act + Assert
      mockMvc.perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized())
          .andExpect(jsonPath("$.message")
              .value("Invalid credentials."));
    }

    @Test
    public void shouldThrowExceptionWhenBlankEmail() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "",
          "superStrongPassword"
      );

      // Act + Assert
      mockMvc.perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Email is required."));

      verifyNoInteractions(authenticationService);
    }

    @Test
    public void shouldThrowExceptionWhenBlankPassword() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "userTest@mail.com",
          ""
      );

      // Act + Assert
      mockMvc.perform(
              post("/auth/login")
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(objectMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message")
              .value("Password is required."));

      verifyNoInteractions(authenticationService);
    }

    @Test
    public void shouldThrowExceptionWhenInvalidBody() throws Exception {
      mockMvc.perform(post("/auth/login")
              .contentType(MediaType.APPLICATION_JSON)
              .content("{}"))
          .andExpect(status().isBadRequest())
          .andExpect(jsonPath("$.message").exists());
    }
  }
}
