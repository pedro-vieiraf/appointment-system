package com.pedrovieira.appointmentsystem.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.user.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testing Register Service Functions")
public class AuthServiceRegisterTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @InjectMocks
  private AuthenticationService authenticationService;

  @Test
  public void shouldRegisterUserSuccessfully() {

    RegisterRequest request = new RegisterRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User result = authenticationService.register(request);

    ArgumentCaptor<User> captor =
        ArgumentCaptor.forClass(User.class);

    verify(userRepository).save(captor.capture());

    User saved = captor.getValue();

    assertEquals("User Resu", saved.getFullName());
    assertEquals("user@email.com", saved.getEmail());
    assertNotEquals(
        "superstrongpassword",
        saved.getPassword()
    );
  }

  @Test
  public void shouldThrowExceptionWhenEmailAlreadyExists() {
    // Arrange
    RegisterRequest request = new RegisterRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.existsByEmail(request.email()))
        .thenReturn(true);

    // Act
    EmailAlreadyExistsException exception = assertThrows(
        EmailAlreadyExistsException.class,
        () -> authenticationService.register(request)
    );

    // Assert
    assertEquals(
        "Email already registered.",
        exception.getMessage()
    );

    verify(userRepository, never())
        .save(any(User.class));
  }

  @Test
  public void shouldEncryptPasswordBeforeSaving() {
    // Arrange
    RegisterRequest request = new RegisterRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(passwordEncoder.encode("superstrongpassword"))
        .thenReturn("encryptedPassword");

    // Act
    authenticationService.register(request);

    // Assert
    ArgumentCaptor<User> captor =
        ArgumentCaptor.forClass(User.class);

    verify(userRepository).save(captor.capture());

    User saved = captor.getValue();

    assertEquals("encryptedPassword", saved.getPassword());

    verify(passwordEncoder)
        .encode("superstrongpassword");
  }

  @Test
  public void shouldNotEncryptPasswordWhenEmailAlreadyExist() {
    // Arrange

    RegisterRequest request = new RegisterRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.existsByEmail(request.email()))
        .thenReturn(true);

    // Act + Assert

    assertThrows(
        EmailAlreadyExistsException.class,
        () -> authenticationService.register(request)
    );

    verify(passwordEncoder, never())
        .encode(any());
  }
}
