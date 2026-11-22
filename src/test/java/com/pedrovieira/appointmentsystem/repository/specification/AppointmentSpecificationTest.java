package com.pedrovieira.appointmentsystem.repository.specification;

import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import jakarta.persistence.criteria.CriteriaBuilder;
import jakarta.persistence.criteria.CriteriaQuery;
import jakarta.persistence.criteria.Expression;
import jakarta.persistence.criteria.Path;
import jakarta.persistence.criteria.Predicate;
import jakarta.persistence.criteria.Root;
import java.time.LocalDateTime;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentMatchers;
import org.springframework.data.jpa.domain.Specification;

@DisplayName("Appointment Specification Tests")
public class AppointmentSpecificationTest {

  @Nested
  @DisplayName("User filter")
  class UserFilter {

    @Test
    @DisplayName("Should filter appointments by user")
    void shouldFilterAppointmentsByUser() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              null
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Path<Object> userPath =
          mock(Path.class);

      when(root.get("user"))
          .thenReturn(userPath);

      when(criteriaBuilder.equal(userPath, user))
          .thenReturn(userPredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      Predicate result = specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(root).get("user");
      verify(criteriaBuilder).equal(userPath, user);
    }
  }

  @Nested
  @DisplayName("Title filter")
  class TitleFilter {

    @Test
    @DisplayName("Should filter appointments by title")
    void shouldFilterAppointmentsByTitle() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              null,
              null
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Predicate titlePredicate =
          mock(Predicate.class);

      Path<User> userPath =
          mock(Path.class);

      Path<String> titlePath =
          mock(Path.class);

      when(root.<User>get("user"))
          .thenReturn(userPath);

      when(root.<String>get("title"))
          .thenReturn(titlePath);

      when(criteriaBuilder.equal(
          userPath,
          user
      )).thenReturn(userPredicate);

      when(criteriaBuilder.lower(
          titlePath
      )).thenReturn(titlePath);

      when(criteriaBuilder.like(
          titlePath,
          "%doctor%"
      )).thenReturn(titlePredicate);

      when(criteriaBuilder.and(
          userPredicate,
          titlePredicate
      )).thenReturn(titlePredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(root)
          .get("title");

      verify(criteriaBuilder)
          .lower(titlePath);

      verify(criteriaBuilder)
          .like(
              titlePath,
              "%doctor%"
          );
    }

    @Test
    @DisplayName("Should ignore a blank title and retain user and date filters")
    void shouldIgnoreBlankTitleAndRetainUserAndDateFilters() {
      User user = new User("John Doe", "john@email.com", "hashed-password");
      LocalDateTime startDate = LocalDateTime.of(2026, 8, 10, 10, 30);
      AppointmentSearchCriteria criteria = new AppointmentSearchCriteria("   ", startDate, null);

      CriteriaBuilder builder = mock(CriteriaBuilder.class);
      CriteriaQuery<Appointment> query = mock(CriteriaQuery.class);
      Root<Appointment> root = mock(Root.class);
      Path<User> userPath = mock(Path.class);
      Path<LocalDateTime> datePath = mock(Path.class);
      Predicate userPredicate = mock(Predicate.class);
      Predicate startDatePredicate = mock(Predicate.class);

      when(root.<User>get("user")).thenReturn(userPath);
      when(root.<LocalDateTime>get("appointmentDate")).thenReturn(datePath);
      when(builder.equal(userPath, user)).thenReturn(userPredicate);
      when(builder.greaterThanOrEqualTo(datePath, startDate)).thenReturn(startDatePredicate);

      AppointmentSpecification.withFilters(user, criteria).toPredicate(root, query, builder);

      verify(builder).and(new Predicate[]{userPredicate, startDatePredicate});
      verify(root, never()).get("title");
    }
  }

  @Nested
  @DisplayName("Date filters")
  class DateFilters {

    @Test
    @DisplayName("Should filter appointments by start date")
    void shouldFilterAppointmentsByStartDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              null
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Predicate startDatePredicate =
          mock(Predicate.class);

      Path<User> userPath =
          mock(Path.class);

      Path<LocalDateTime> datePath =
          mock(Path.class);

      when(root.<User>get("user"))
          .thenReturn(userPath);

      when(root.<LocalDateTime>get("appointmentDate"))
          .thenReturn(datePath);

      when(criteriaBuilder.equal(
          userPath,
          user
      )).thenReturn(userPredicate);

      when(criteriaBuilder.greaterThanOrEqualTo(
          datePath,
          startDate
      )).thenReturn(startDatePredicate);

      when(criteriaBuilder.and(
          userPredicate,
          startDatePredicate
      )).thenReturn(startDatePredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(root)
          .get("appointmentDate");

      verify(criteriaBuilder)
          .greaterThanOrEqualTo(
              datePath,
              startDate
          );
    }

    @Test
    @DisplayName("Should filter appointments by end date")
    void shouldFilterAppointmentsByEndDate() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(5);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              null,
              endDate
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Predicate endDatePredicate =
          mock(Predicate.class);

      Path<User> userPath =
          mock(Path.class);

      Path<LocalDateTime> datePath =
          mock(Path.class);

      when(root.<User>get("user"))
          .thenReturn(userPath);

      when(root.<LocalDateTime>get("appointmentDate"))
          .thenReturn(datePath);

      when(criteriaBuilder.equal(
          userPath,
          user
      )).thenReturn(userPredicate);

      when(criteriaBuilder.lessThanOrEqualTo(
          datePath,
          endDate
      )).thenReturn(endDatePredicate);

      when(criteriaBuilder.and(
          userPredicate,
          endDatePredicate
      )).thenReturn(endDatePredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(root)
          .get("appointmentDate");

      verify(criteriaBuilder)
          .lessThanOrEqualTo(
              datePath,
              endDate
          );
    }

    @Test
    @DisplayName("Should filter appointments by date range")
    void shouldFilterAppointmentsByDateRange() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(5);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              null,
              startDate,
              endDate
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Predicate startDatePredicate =
          mock(Predicate.class);

      Predicate endDatePredicate =
          mock(Predicate.class);

      Path<Object> userPath =
          mock(Path.class);

      Path<LocalDateTime> datePath =
          mock(Path.class);

      when(root.get("user"))
          .thenReturn(userPath);

      when(root.<LocalDateTime>get("appointmentDate"))
          .thenReturn(datePath);

      when(criteriaBuilder.equal(userPath, user))
          .thenReturn(userPredicate);

      when(criteriaBuilder.greaterThanOrEqualTo(
          datePath,
          startDate
      )).thenReturn(startDatePredicate);

      when(criteriaBuilder.lessThanOrEqualTo(
          datePath,
          endDate
      )).thenReturn(endDatePredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(criteriaBuilder)
          .greaterThanOrEqualTo(
              datePath,
              startDate
          );

      verify(criteriaBuilder)
          .lessThanOrEqualTo(
              datePath,
              endDate
          );
    }
  }

  @Nested
  @DisplayName("Combined filters")
  class CombinedFilters {

    @Test
    @DisplayName("Should filter appointments by title and date range")
    void shouldFilterAppointmentsByTitleAndDateRange() {
      // Arrange
      User user = new User(
          "John Doe",
          "john@email.com",
          "hashed-password"
      );

      LocalDateTime startDate =
          LocalDateTime.now().plusDays(2);

      LocalDateTime endDate =
          LocalDateTime.now().plusDays(5);

      AppointmentSearchCriteria criteria =
          new AppointmentSearchCriteria(
              "Doctor",
              startDate,
              endDate
          );

      CriteriaBuilder criteriaBuilder =
          mock(CriteriaBuilder.class);

      CriteriaQuery<Appointment> criteriaQuery =
          mock(CriteriaQuery.class);

      Root<Appointment> root =
          mock(Root.class);

      Predicate userPredicate =
          mock(Predicate.class);

      Predicate titlePredicate =
          mock(Predicate.class);

      Predicate startDatePredicate =
          mock(Predicate.class);

      Predicate endDatePredicate =
          mock(Predicate.class);

      Predicate combinedPredicate =
          mock(Predicate.class);

      Path<User> userPath =
          mock(Path.class);

      Path<String> titlePath =
          mock(Path.class);

      Path<LocalDateTime> datePath =
          mock(Path.class);

      Expression<String> lowerTitle =
          mock(Expression.class);

      when(root.<User>get("user"))
          .thenReturn(userPath);

      when(root.<String>get("title"))
          .thenReturn(titlePath);

      when(root.<LocalDateTime>get("appointmentDate"))
          .thenReturn(datePath);

      when(criteriaBuilder.equal(
          userPath,
          user
      )).thenReturn(userPredicate);

      when(criteriaBuilder.lower(
          titlePath
      )).thenReturn(lowerTitle);

      when(criteriaBuilder.like(
          ArgumentMatchers.<Expression<String>>any(),
          eq("%doctor%")
      )).thenReturn(titlePredicate);

      when(criteriaBuilder.greaterThanOrEqualTo(
          datePath,
          startDate
      )).thenReturn(startDatePredicate);

      when(criteriaBuilder.lessThanOrEqualTo(
          datePath,
          endDate
      )).thenReturn(endDatePredicate);

      when(criteriaBuilder.and(
          userPredicate,
          titlePredicate,
          startDatePredicate,
          endDatePredicate
      )).thenReturn(combinedPredicate);

      Specification<Appointment> specification =
          AppointmentSpecification.withFilters(
              user,
              criteria
          );

      // Act
      specification.toPredicate(
          root,
          criteriaQuery,
          criteriaBuilder
      );

      // Assert
      verify(criteriaBuilder)
          .equal(userPath, user);

      verify(criteriaBuilder)
          .lower(titlePath);

      verify(criteriaBuilder)
          .like(
              lowerTitle,
              "%doctor%"
          );

      verify(criteriaBuilder)
          .greaterThanOrEqualTo(
              datePath,
              startDate
          );

      verify(criteriaBuilder)
          .lessThanOrEqualTo(
              datePath,
              endDate
          );

      verify(criteriaBuilder)
          .and(
              userPredicate,
              titlePredicate,
              startDatePredicate,
              endDatePredicate
          );
    }
  }
}