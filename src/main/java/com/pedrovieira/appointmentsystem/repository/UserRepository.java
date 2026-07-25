package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * User repository.
 */
@Repository
public interface UserRepository extends JpaRepository<User, Long> {

  boolean existsByEmail(String email);
}
