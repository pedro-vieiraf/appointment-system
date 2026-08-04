package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.user.InvalidCredentialsException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.security.JwtService;
import java.util.Optional;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testing Login Service Functions")
public class AuthServiceLoginTest {

  @InjectMocks
  private AuthenticationService authenticationService;

  @Mock
  private UserRepository userRepository;

  @Mock
  private PasswordEncoder passwordEncoder;

  @Mock
  private JwtService jwtService;

  @Nested
  @DisplayName("Happy Path")
  class HappyPath {

    @Test
    public void shouldLoginSuccessfully() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "userTest@mail.com",
          "superstrongpassword"
      );

      User user = new User();
      user.setId(1L);
      user.setEmail("userTest@mail.com");
      user.setFullName("User Resu");
      user.setPassword("hashedPassword");

      when(userRepository.findByEmail(request.email()))
          .thenReturn(Optional.of(user));

      when(passwordEncoder.matches(
          request.password(),
          user.getPassword()
      ))
          .thenReturn(true);

      when(jwtService.generateToken(user))
          .thenReturn("fake.jwt.token");
      when(jwtService.getExpirationInSeconds())
          .thenReturn(900L);

      // Act
      LoginResponse response = authenticationService.login(request);

      // Assert
      assertEquals("fake.jwt.token", response.accessToken());
      assertEquals("Bearer", response.tokenType());
      assertEquals(900L, response.expiresIn());
    }
  }

  @Nested
  @DisplayName("Validation Errors")
  class ValidationErrors {

    @Test
    public void shouldThrowExceptionWhenEmailIsInvalid() throws Exception {
      // Arrange
      LoginRequest request = new LoginRequest(
          "userTestWrong@mail.com",
          "superStrongpassword"
      );
      when(userRepository.findByEmail(request.email()))
          .thenReturn(Optional.empty());

      // Act + Assert
      InvalidCredentialsException exception =
          assertThrows(
              InvalidCredentialsException.class,
              () -> authenticationService.login(request)
          );

      assertEquals(
          "Invalid credentials.",
          exception.getMessage()
      );

      verify(jwtService, never())
          .generateToken(any(User.class));
    }

    @Test
    void shouldThrowExceptionWhenPasswordIsInvalid() {

      // Arrange
      LoginRequest request = new LoginRequest(
          "userTest@mail.com",
          "superWrongPassword"
      );

      User user = new User();
      user.setId(1L);
      user.setEmail("userTest@mail.com");
      user.setFullName("User Resu");
      user.setPassword("hashedPassword");

      when(userRepository.findByEmail(request.email()))
          .thenReturn(Optional.of(user));

      when(passwordEncoder.matches(
          request.password(),
          user.getPassword()
      ))
          .thenReturn(false);

      // Act + Assert
      InvalidCredentialsException exception =
          assertThrows(
              InvalidCredentialsException.class,
              () -> authenticationService.login(request)
          );

      assertEquals(
          "Invalid credentials.",
          exception.getMessage()
      );

      verify(jwtService, never())
          .generateToken(any(User.class));
    }
  }
}
