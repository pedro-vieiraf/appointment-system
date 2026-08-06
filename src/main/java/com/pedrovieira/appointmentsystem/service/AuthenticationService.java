package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.LoginRequest;
import com.pedrovieira.appointmentsystem.dto.request.RegisterRequest;
import com.pedrovieira.appointmentsystem.dto.response.LoginResponse;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.user.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.exception.user.EmailNotFoundException;
import com.pedrovieira.appointmentsystem.exception.user.InvalidCredentialsException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import com.pedrovieira.appointmentsystem.security.JwtService;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service that will handle users authentication.
 */
@Service
@RequiredArgsConstructor
public class AuthenticationService {

  private final JwtService jwtService;
  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * CreateUser function.
   */
  public User register(RegisterRequest request) throws EmailAlreadyExistsException {

    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException();
    }

    String hashedPassword = passwordEncoder.encode(request.password());

    User user = new User(
        request.fullName(),
        request.email(),
        hashedPassword
    );

    user.setPassword(hashedPassword);

    return userRepository.save(user);
  }

  /**
   * GetUserByEmail function.
   */
  public User getUserByEmail(String email) {
    return userRepository.findByEmail(email)
        .orElseThrow(() -> new EmailNotFoundException());
  }

  /**
   * Login function.
   */
  public LoginResponse login(LoginRequest request) {
    User user = userRepository.findByEmail(request.email())
        .orElseThrow(() -> new InvalidCredentialsException("Invalid credentials."));

    if (!passwordEncoder.matches(request.password(), user.getPassword())) {
      throw new InvalidCredentialsException("Invalid credentials.");
    }

    String token = jwtService.generateToken(user);

    return new LoginResponse(token, "Bearer", jwtService.getExpirationInSeconds());
  }
}