package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.CreateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * Service that will handle users.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * CreateUser function.
   */
  public User createUser(CreateUserRequest request) throws EmailAlreadyExistsException {
    if (userRepository.existsByEmail(request.email())) {
      throw new EmailAlreadyExistsException(request.email());
    }

    User user = new User(
        request.fullName(),
        request.email(),
        request.password()
    );


    String hashedPassword = passwordEncoder.encode(request.password());

    user.setPassword(hashedPassword);

    return userRepository.save(user);
  }
}