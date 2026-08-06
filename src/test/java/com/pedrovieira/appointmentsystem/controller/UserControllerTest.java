package com.pedrovieira.appointmentsystem.controller;

import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.security.JwtAuthenticationFilter;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@DisplayName("UserController Tests")
public class UserControllerTest {

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @Autowired
  private WebApplicationContext context;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Nested
  @DisplayName("Happy Path")
  class HappyPath {

    @Test
    public void shouldReturnCurrentUserData() throws Exception {
      // Arrange
      User authenticatedUser = new User(
          "User Resu",
          "user@mail.com",
          "hashed-password"
      );
      authenticatedUser.setId(1L);

      // Act + Assert
      mockMvc.perform(get("/users/me").with(user(authenticatedUser)))
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1))
          .andExpect(jsonPath("$.fullName").value("User Resu"))
          .andExpect(jsonPath("$.email").value("user@mail.com"));
    }
  }

  @Nested
  @DisplayName("Security")
  class Security {

    @Test
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
      mockMvc.perform(get("/users/me"))
          .andExpect(status().isUnauthorized());
    }
  }
}
