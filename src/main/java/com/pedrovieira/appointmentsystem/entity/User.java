package com.pedrovieira.appointmentsystem.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.Collection;
import java.util.List;

import lombok.Getter;
import lombok.Setter;
import org.jspecify.annotations.Nullable;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

/**
 * User entity definition.
 */
@Entity
@Table(name = "users")
public class User implements UserDetails {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @Getter
  @Setter
  @Column(unique = true, nullable = false)
  private String email;

  @Getter
  @Setter
  @Column(nullable = false)
  private String fullName;

  @Column(nullable = false)
  @Setter
  private String password;

  private LocalDateTime createdAt;
  private LocalDateTime updatedAt;

  /**
   * Empty constructor for spring.
   */
  public User() {
  }

  /**
   * User Constructor for general use.
   */
  public User(String fullName,  String email, String password) {
    this.fullName = fullName;
    this.email = email;
    this.password = password;
  }

  @Override
  public Collection<? extends GrantedAuthority> getAuthorities() {
    return List.of();
  }

  @Override
  public @Nullable String getPassword() {
    return this.password;
  }

  @Override
  public String getUsername() {
    return email;
  }
}
