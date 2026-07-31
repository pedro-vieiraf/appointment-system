package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.exception.user.EmailAlreadyExistsException;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.security.crypto.password.PasswordEncoder;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
@DisplayName("Testing UserService functions")
public class UserServiceTest {

  @Mock
  private UserRepository userRepository;

  @Mock
  PasswordEncoder passwordEncoder;

  @InjectMocks
  private UserService userService;

  @Test
  public void shouldRegisterUserSuccessfully() {

    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.save(any(User.class)))
        .thenAnswer(invocation -> invocation.getArgument(0));

    User result = userService.createUser(request);

    ArgumentCaptor<User> captor =
        ArgumentCaptor.forClass(User.class);

    verify(userRepository).save(captor.capture());

    User saved = captor.getValue();

    assertEquals("User Resu", saved.getFullName());
    assertEquals("user@email.com", saved.getEmail());
  }

  @Test
  public void shouldThrowExceptionWhenEmailAlreadyExists() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.existsByEmail(request.email()))
        .thenReturn(true);

    // Act
    EmailAlreadyExistsException exception = assertThrows(
        EmailAlreadyExistsException.class,
        () -> userService.createUser(request)
    );

    // Assert
    assertEquals(
        "Email already registered.",
        exception.getMessage()
    );
  }

  @Test
  public void shouldEncryptPasswordBeforeSaving() {
    // Arrange
    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(passwordEncoder.encode("superstrongpassword"))
        .thenReturn("encryptedPassword");

    // Act
      userService.createUser(request);

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

    CreateUserRequest request = new CreateUserRequest(
        "User Resu",
        "user@email.com",
        "superstrongpassword"
    );

    when(userRepository.existsByEmail(request.email()))
        .thenReturn(true);

    // Act + Assert

    assertThrows(
        EmailAlreadyExistsException.class,
        () -> userService.createUser(request)
    );

    verify(passwordEncoder, never())
        .encode(any());
  }
}
