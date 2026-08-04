package com.pedrovieira.appointmentsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.mock.web.MockHttpServletRequest;
import org.springframework.mock.web.MockHttpServletResponse;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;

@ExtendWith(MockitoExtension.class)
public class JwtAuthenticationFilterTest {

  @Mock
  private JwtService jwtService;

  @Mock
  private UserRepository userRepository;

  private JwtAuthenticationFilter filter;

  @BeforeEach
  void setUp() {
    filter = new JwtAuthenticationFilter(
        jwtService,
        userRepository
    );
  }

  @Test
  void shouldAuthenticateUserWhenTokenIsValid() throws Exception {

    // Arrange

    MockHttpServletRequest request =
        new MockHttpServletRequest();

    request.addHeader(
        "Authorization",
        "Bearer fake.jwt.token"
    );

    MockHttpServletResponse response =
        new MockHttpServletResponse();

    FilterChain chain = mock(FilterChain.class);

    User user = new User();
    user.setId(1L);

    when(jwtService.extractUserId("fake.jwt.token"))
        .thenReturn(1L);

    when(userRepository.findById(1L))
        .thenReturn(Optional.of(user));

    // Act

    filter.doFilter(
        request,
        response,
        chain
    );

    // Assert

    Authentication authentication =
        SecurityContextHolder
            .getContext()
            .getAuthentication();

    assertNotNull(authentication);

    assertEquals(
        user,
        authentication.getPrincipal()
    );
  }
}
