package com.pedrovieira.appointmentsystem.security;

import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import java.io.IOException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.filter.OncePerRequestFilter;

/**
 * AuthenticationFilter.
 */
@Component
public class JwtAuthenticationFilter extends OncePerRequestFilter {

  private final JwtService jwtService;
  private final UserRepository userRepository;

  /**
   * Authentication Filter Constructor.
   */
  public JwtAuthenticationFilter(
      JwtService jwtService,
      UserRepository userRepository
  ) {
    this.jwtService = jwtService;
    this.userRepository = userRepository;
  }

  @Override
  protected void doFilterInternal(
      HttpServletRequest request,
      HttpServletResponse response,
      FilterChain filterChain
  ) throws ServletException, IOException {

    String token = extractToken(request);

    if (token != null) {
      authenticateUser(token);
    }

    filterChain.doFilter(request, response);
  }

  private String extractToken(HttpServletRequest request) {
    String authHeader = request.getHeader("Authorization");

    if (authHeader == null || !authHeader.startsWith("Bearer ")) {
      return null;
    }

    return authHeader.substring(7);
  }

  private void setAuthentication(User user) {

    Authentication authentication =
        new UsernamePasswordAuthenticationToken(
            user,
            null,
            user.getAuthorities()
        );

    SecurityContextHolder
        .getContext()
        .setAuthentication(authentication);
  }

  private void authenticateUser(String token) {

    if (!jwtService.isValidToken(token)) {
      return;
    }

    Long userId = jwtService.extractUserId(token);

    userRepository.findById(userId)
        .ifPresent(this::setAuthentication);
  }
}
