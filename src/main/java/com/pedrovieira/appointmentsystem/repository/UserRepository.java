package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.User;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * User repository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  /**
   * Boolean that returns if email already is registered.
   */
  boolean existsByEmail(String email);

  /**
   * Find by Email function.
   */
  Optional<User> findByEmail(String email);
}
