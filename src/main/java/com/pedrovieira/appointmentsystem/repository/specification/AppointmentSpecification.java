package com.pedrovieira.appointmentsystem.repository.specification;

import com.pedrovieira.appointmentsystem.dto.request.AppointmentSearchCriteria;
import com.pedrovieira.appointmentsystem.entity.Appointment;
import com.pedrovieira.appointmentsystem.entity.User;
import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import org.springframework.data.jpa.domain.Specification;

/**
 * JPA specification for dynamically filtering appointments.
 */
public class AppointmentSpecification {

  /**
   * Creates a specification based on the provided search criteria.
   */
  public static Specification<Appointment> withFilters(
      User user,
      AppointmentSearchCriteria criteria
  ) {
    return (root, query, builder) -> {
      List<Predicate> predicates = new ArrayList<>();

      predicates.add(
          builder.equal(root.get("user"), user)
      );

      if (criteria.title() != null
          && !criteria.title().isBlank()) {
        predicates.add(
            builder.like(
                builder.lower(root.get("title")),
                "%" + criteria.title().toLowerCase(Locale.ROOT) + "%"
            )
        );
      }

      if (criteria.startDate() != null) {
        predicates.add(
            builder.greaterThanOrEqualTo(
                root.get("appointmentDate"),
                criteria.startDate()
            )
        );
      }

      if (criteria.endDate() != null) {
        predicates.add(
            builder.lessThanOrEqualTo(
                root.get("appointmentDate"),
                criteria.endDate()
            )
        );
      }

      return builder.and(
          predicates.toArray(new Predicate[0])
      );
    };
  }
}
