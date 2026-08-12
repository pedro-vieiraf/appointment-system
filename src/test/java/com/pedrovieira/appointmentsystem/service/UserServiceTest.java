package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.UpdateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
@DisplayName("User Service Tests")
public class UserServiceTest {

  @InjectMocks
  private UserService userService;

  @Mock
  private UserRepository userRepository;

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
}
