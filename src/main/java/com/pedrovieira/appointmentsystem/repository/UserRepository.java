package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.User;
import jakarta.persistence.LockModeType;
import java.util.Optional;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Lock;
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

  /**
   * Verify if email is already registered by another user.
   */
  boolean existsByEmailAndIdNot(String email, Long id);

  /**
   * Serializes changes to the schedule and appointments for this account.
   */
  @Lock(LockModeType.PESSIMISTIC_WRITE)
  Optional<User> findForSchedulingUpdateById(Long id);
}
