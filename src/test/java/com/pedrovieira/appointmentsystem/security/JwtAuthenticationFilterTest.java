package com.pedrovieira.appointmentsystem.security;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.verifyNoInteractions;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.entity.User;
import com.pedrovieira.appointmentsystem.repository.UserRepository;
import jakarta.servlet.FilterChain;
import java.util.Optional;

import org.junit.jupiter.api.AfterEach;
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

  @AfterEach
  void cleanSecurityContext() {
    SecurityContextHolder.clearContext();
  }

  @Test
  void shouldAuthenticateRequestWhenJwtIsValid() throws Exception {
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

    when(jwtService.isValidToken("fake.jwt.token"))
        .thenReturn(true);

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

  @Test
  public void shouldContinueFilterChainWhenTokenIsMissing() throws Exception {
    // Arrange
    MockHttpServletRequest request =
        new MockHttpServletRequest();

    MockHttpServletResponse response =
        new MockHttpServletResponse();

    FilterChain filterChain =
        mock(FilterChain.class);

    // Act
    filter.doFilter(
        request,
        response,
        filterChain
    );

    // Assert
    verify(filterChain)
        .doFilter(request, response);
  }

  @Test
  void shouldIgnoreAuthorizationHeaderWithoutBearerScheme() throws Exception {
    MockHttpServletRequest request = new MockHttpServletRequest();
    request.addHeader("Authorization", "Basic dXNlcjpwYXNz");
    MockHttpServletResponse response = new MockHttpServletResponse();
    FilterChain chain = mock(FilterChain.class);

    filter.doFilter(request, response, chain);

    assertNull(SecurityContextHolder.getContext().getAuthentication());
    verifyNoInteractions(jwtService, userRepository);
    verify(chain).doFilter(request, response);
  }

  @Test
  public void shouldNotAuthenticateUserWhenJwtIsInvalid() throws Exception {
    // Arrange
    MockHttpServletRequest request =
        new MockHttpServletRequest();

    request.addHeader(
        "Authorization",
        "Bearer invalid.jwt.token"
    );

    MockHttpServletResponse response =
        new MockHttpServletResponse();

    FilterChain chain =
        mock(FilterChain.class);

    when(jwtService.isValidToken("invalid.jwt.token"))
        .thenReturn(false);

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

    assertNull(authentication);

    verify(chain)
        .doFilter(request, response);
  }

  @Test
  public void shouldContinueFilterChainWhenUserDoesNotExist() throws Exception {
    // Arrange
    MockHttpServletRequest request =
        new MockHttpServletRequest();

    request.addHeader(
        "Authorization",
        "Bearer fake.jwt.token"
    );

    MockHttpServletResponse response =
        new MockHttpServletResponse();

    FilterChain chain =
        mock(FilterChain.class);

    when(jwtService.isValidToken("fake.jwt.token"))
        .thenReturn(true);

    when(jwtService.extractUserId("fake.jwt.token"))
        .thenReturn(1L);

    when(userRepository.findById(1L))
        .thenReturn(Optional.empty());

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

    assertNull(authentication);

    verify(chain)
        .doFilter(request, response);
  }
}
