package com.pedrovieira.appointmentsystem.service;

import com.pedrovieira.appointmentsystem.dto.request.ChangePasswordRequest;
import com.pedrovieira.appointmentsystem.dto.request.UpdateUserRequest;
import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.exception.users.EmailAlreadyExistsException;
import com.pedrovieira.appointmentsystem.exception.users.InvalidPasswordException;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

/**
 * User Service.
 */
@Service
@RequiredArgsConstructor
public class UserService {

  private final UserRepository userRepository;
  private final PasswordEncoder passwordEncoder;

  /**
   * Update User.
   */
  public User update(User user, UpdateUserRequest request) {
    if (request.fullName() != null) {
      user.setFullName(request.fullName());
    }

    if (request.email() != null) {
      if (userRepository.existsByEmailAndIdNot(
          request.email(), user.getId()
      )) {
        throw new EmailAlreadyExistsException();
      }

      user.setEmail(request.email());
    }

    return userRepository.save(user);
  }

  /**
   * Update password.
   */
  public void updatePassword(
      User user,
      ChangePasswordRequest request
  ) {
    if (!passwordEncoder.matches(
        request.currentPassword(),
        user.getPassword()
    )) {
      throw new InvalidPasswordException();
    }

    String encodedPassword =
        passwordEncoder.encode(request.newPassword());

    user.setPassword(encodedPassword);

    userRepository.save(user);
  }
}
