package com.pedrovieira.appointmentsystem.controller;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.pedrovieira.appointmentsystem.config.SecurityConfig;
import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
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
        "123456"
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
    assertEquals("123456", sent.password());
  }

  @Test
  public void shouldReturnErrorIfBlankEmail() throws Exception {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "",
        "123456"
    );

    when(userService.createUser(any()))
        .thenThrow(IllegalArgumentException.class);

    mockMvc.perform(
        post("/auth/register")
            .contentType(MediaType.APPLICATION_JSON)
            .content(objectMapper.writeValueAsString(request))
        )
        .andExpect(status().isBadRequest())
        .andExpect(jsonPath("$.message").value("Email is required."))
        .andExpect(jsonPath("$.error").value("Email is required."));
  }

}
