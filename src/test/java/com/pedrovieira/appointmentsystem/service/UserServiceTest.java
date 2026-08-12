package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.ChangePasswordRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.exception.users.InvalidPasswordException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
public class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;


  @Nested
  @DisplayName("Update User")
  class UpdateUserTests {

    @Test
    @DisplayName("Should update both fields successfully")
    void shouldUpdateUserSuccessfully() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      UpdateUserRequest request = new UpdateUserRequest(
          "John Smith",
          "johnsmith@email.com"
      );

      when(userRepository.save(user)).thenReturn(user);

      // Act
      User result = userService.update(user, request);

      // Assert
      assertEquals("John Smith", result.getFullName());
      assertEquals("johnsmith@email.com", result.getEmail());

      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should update user full name successfully")
    void shouldUpdateUserFullNameSuccessfully() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      UpdateUserRequest request = new UpdateUserRequest(
          "John Smith",
          null
      );

      when(userRepository.save(user)).thenReturn(user);

      // Act
      User result = userService.update(user, request);

      // Assert
      assertEquals("John Smith", result.getFullName());
      assertEquals("john@email.com", result.getEmail());

      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should update user email successfully")
    void shouldUpdateUserEmailSuccessfully() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      UpdateUserRequest request = new UpdateUserRequest(
          null,
          "johnsmith@email.com"
      );

      when(userRepository.save(user)).thenReturn(user);

      // Act
      User result = userService.update(user, request);

      // Assert
      assertEquals("John Doe", result.getFullName());
      assertEquals("johnsmith@email.com", result.getEmail());

      verify(userRepository).save(user);
    }

    @Test
    @DisplayName("Should not update to an existing email")
    void shouldNotUpdateUserEmailToAnExistingEmail() {
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

      when(userRepository.existsByEmailAndIdNot(
          request.email(),
          user.getId()
      )).thenReturn(true);

      // Act + Assert
      EmailAlreadyExistsException exception =
          assertThrows(
              EmailAlreadyExistsException.class,
              () -> userService.update(user, request)
          );

      assertEquals(
          "Email already registered.",
          exception.getMessage()
      );

      verify(userRepository)
          .existsByEmailAndIdNot(
              request.email(),
              user.getId()
          );

      verify(userRepository, never())
          .save(any(User.class));
    }

    @Test
    @DisplayName("Should update user with their current email")
    void shouldUpdateUserWithTheirCurrentEmail() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );
      user.setId(1L);

      UpdateUserRequest request = new UpdateUserRequest(
          null,
          "john@email.com"
      );

      when(userRepository.existsByEmailAndIdNot(
          request.email(),
          user.getId()
      )).thenReturn(false);

      when(userRepository.save(user))
          .thenReturn(user);

      // Act
      User result = userService.update(user, request);

      // Assert
      assertEquals("John Doe", result.getFullName());
      assertEquals("john@email.com", result.getEmail());

      verify(userRepository)
          .existsByEmailAndIdNot(
              request.email(),
              user.getId()
          );

      verify(userRepository)
          .save(user);
    }
  }

  @Nested
  @DisplayName("Update Password")
  class UpdatePassword {

    @Test
    @DisplayName("Should update password successfully")
    void shouldUpdatePasswordSuccessfully() {
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

      when(passwordEncoder.matches(anyString(), anyString()))
          .thenReturn(true);

      when(passwordEncoder.encode(anyString()))
          .thenReturn("new-hashed-password");

      // Act
      userService.updatePassword(user, request);

      // Assert
      assertEquals(
          "new-hashed-password",
          user.getPassword()
      );

      verify(passwordEncoder)
          .matches(anyString(), anyString());

      verify(passwordEncoder)
          .encode("new-password");

      verify(userRepository)
          .save(user);
    }

    @Test
    @DisplayName("Should reject password change when current password is incorrect")
    void shouldRejectPasswordChangeWhenCurrentPasswordIsIncorrect() {
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

      when(passwordEncoder.matches(
          request.currentPassword(),
          user.getPassword()
      )).thenReturn(false);

      // Act + Assert
      InvalidPasswordException exception =
          assertThrows(
              InvalidPasswordException.class,
              () -> userService.updatePassword(user, request)
          );

      assertEquals(
          "Current password is incorrect.",
          exception.getMessage()
      );

      verify(passwordEncoder)
          .matches(
              request.currentPassword(),
              user.getPassword()
          );

      verify(passwordEncoder, never())
          .encode(anyString());

      verify(userRepository, never())
          .save(any(User.class));
    }

    @Test
    @DisplayName("Should encode new password before saving")
    void shouldEncodeNewPasswordBeforeSaving() {
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

      when(passwordEncoder.matches(
          "old-password",
          "old-hashed-password"
      )).thenReturn(true);

      when(passwordEncoder.encode("new-password"))
          .thenReturn("new-hashed-password");

      // Act
      userService.updatePassword(user, request);

      // Assert
      assertEquals(
          "new-hashed-password",
          user.getPassword()
      );

      verify(passwordEncoder)
          .encode("new-password");

      verify(userRepository)
          .save(user);
    }
  }
}
