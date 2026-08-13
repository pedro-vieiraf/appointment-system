package com.pedrovieira.appointmentsystem.controller;

import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.user;
import static org.springframework.security.test.web.servlet.setup.SecurityMockMvcConfigurers.springSecurity;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import com.pedrovieira.appointmentsystem.dto.request.ChangePasswordRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.exception.users.InvalidPasswordException;
import com.pedrovieira.appointmentsystem.security.JwtAuthenticationFilter;
import com.pedrovieira.appointmentsystem.service.UserService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.boot.webmvc.test.autoconfigure.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.setup.MockMvcBuilders;
import org.springframework.web.context.WebApplicationContext;
import tools.jackson.databind.json.JsonMapper;

@WebMvcTest(UserController.class)
@AutoConfigureMockMvc
@DisplayName("UserController Tests")
public class UserControllerTest {

  @MockitoBean
  private JwtAuthenticationFilter jwtAuthenticationFilter;

  @MockitoBean
  private UserService userService;

  @Autowired
  private WebApplicationContext context;

  @Autowired
  private JsonMapper jsonMapper;

  private MockMvc mockMvc;

  @BeforeEach
  void setup() {
    mockMvc = MockMvcBuilders
        .webAppContextSetup(context)
        .apply(springSecurity())
        .build();
  }

  @Nested
  @DisplayName("GET User")
  class GetUser {

    @Test
    @DisplayName("Should return current user data")
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

    @Test
    @DisplayName("Should return unauthorized when user is not authenticated")
    void shouldReturnUnauthorizedWhenUserIsNotAuthenticated() throws Exception {
      mockMvc.perform(get("/users/me"))
          .andExpect(status().isUnauthorized());
    }
  }

  @Nested
  @DisplayName("PATCH User")
  class PatchUser {

    @Test
    @DisplayName("Should update both fields")
    void shouldUpdateBothFields() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      user.setId(1L);

      UpdateUserRequest request = new UpdateUserRequest(
          "John Smith",
          "john.smith@email.com"
      );

      User updatedUser = new User(
          "John Smith",
          "john.smith@email.com",
          "hashed-password"
      );
      updatedUser.setId(1L);

      when(userService.update(user, request))
          .thenReturn(updatedUser);

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.fullName").value("John Smith"))
          .andExpect(jsonPath("$.email").value("john.smith@email.com"));

      verify(userService).update(user, request);
    }

    @Test
    @DisplayName("Should update only user full name")
    void shouldUpdateOnlyFullName() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      user.setId(1L);

      UpdateUserRequest request = new UpdateUserRequest(
          "John Smith",
          null
      );

      User updatedUser = new User(
          "John Smith",
          "john@email.com",
          "hashed-password"
      );
      updatedUser.setId(1L);

      when(userService.update(user, request))
          .thenReturn(updatedUser);

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.fullName").value("John Smith"))
          .andExpect(jsonPath("$.email").value("john@email.com"));

      verify(userService)
          .update(user, request);
    }

    @Test
    @DisplayName("Should update only user email")
    void shouldUpdateOnlyEmail() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      user.setId(1L);

      UpdateUserRequest request = new UpdateUserRequest(
          null,
          "john.smith@email.com"
      );

      User updatedUser = new User(
          "John Doe",
          "john.smith@email.com",
          "hashed-password"
      );
      updatedUser.setId(1L);

      when(userService.update(user, request))
          .thenReturn(updatedUser);

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isOk())
          .andExpect(jsonPath("$.id").value(1L))
          .andExpect(jsonPath("$.fullName").value("John Doe"))
          .andExpect(jsonPath("$.email").value("john.smith@email.com"));

      verify(userService)
          .update(user, request);
    }

    @Test
    @DisplayName("Should return 400 when full name is below minimum length")
    void shouldReturn400WhenFullNameIsBelowMinimumLength() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      UpdateUserRequest request = new UpdateUserRequest(
          "Jo",
          null
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Full name must contain between 3 and 50 characters."
                  )
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when full name is above maximum length")
    void shouldReturn400WhenFullNameIsAboveMaximumLength() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      String fullName = "A".repeat(51);

      UpdateUserRequest request = new UpdateUserRequest(
          fullName,
          null
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value(
                      "Full name must contain between 3 and 50 characters."
                  )
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when email is invalid")
    void shouldReturn400WhenEmailIsInvalid() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      UpdateUserRequest request = new UpdateUserRequest(
          null,
          "invalid-email"
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("Email must be valid.")
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 409 when email already exists")
    void shouldReturn409WhenEmailAlreadyExists() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      user.setId(1L);

      UpdateUserRequest request = new UpdateUserRequest(
          null,
          "mary@email.com"
      );

      when(userService.update(user, request))
          .thenThrow(new EmailAlreadyExistsException());

      // Act + Assert
      mockMvc.perform(
              patch("/users/me")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isConflict())
          .andExpect(
              jsonPath("$.message")
                  .value("Email already registered.")
          );

      verify(userService)
          .update(user, request);
    }
  }

  @Nested
  @DisplayName("PATCH Password")
  class PatchPassword {

    @Test
    @DisplayName("Should update password")
    void shouldUpdatePassword() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "old-password",
          "new-password"
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isNoContent())
          .andExpect(content().string(""));

      // Verify
      verify(userService)
          .updatePassword(
              user,
              request
          );
    }

    @Test
    @DisplayName("Should return 400 when new password is blank")
    void shouldReturn400WhenNewPasswordIsBlank() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "old-password",
          ""
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("New password is required.")
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when new password is too short")
    void shouldReturn400WhenNewPasswordIsTooShort() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "old-password",
          "1234567"
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("New password must have at least 8 characters.")
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when current password is blank")
    void shouldReturn400WhenCurrentPasswordIsBlank() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "",
          "new-password"
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("Current password is required.")
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when current password is too short")
    void shouldReturn400WhenCurrentPasswordIsTooShort() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "1234567",
          "new-password"
      );

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isBadRequest())
          .andExpect(
              jsonPath("$.message")
                  .value("Current password must have at least 8 characters.")
          );

      verifyNoInteractions(userService);
    }

    @Test
    @DisplayName("Should return 400 when current password is incorrect")
    void shouldReturn400WhenCurrentPasswordIsIncorrect() throws Exception {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "old-hashed-password"
      );

      ChangePasswordRequest request = new ChangePasswordRequest(
          "wrong-password",
          "new-password"
      );

      doThrow(new InvalidPasswordException())
          .when(userService)
          .updatePassword(user, request);

      // Act + Assert
      mockMvc.perform(
              patch("/users/me/password")
                  .with(user(user))
                  .with(csrf())
                  .contentType(MediaType.APPLICATION_JSON)
                  .content(jsonMapper.writeValueAsString(request))
          )
          .andExpect(status().isUnauthorized())
          .andExpect(
              jsonPath("$.message")
                  .value("Current password is incorrect.")
          );

      verify(userService)
          .updatePassword(user, request);
    }
  }
}
