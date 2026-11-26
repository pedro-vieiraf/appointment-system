package com.pedrovieira.appointmentsystem.repository;

import com.pedrovieira.appointmentsystem.entity.BookingVerification;
import java.time.Instant;
import java.util.UUID;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;

/** Stores unverified public booking requests. */
public interface BookingVerificationRepository extends JpaRepository<BookingVerification, UUID> {

  /** Deletes tokens that can no longer be used. */
  @Modifying
  @Query("delete from BookingVerification v where v.expiresAt <= :now")
  void deleteExpired(Instant now);
}
