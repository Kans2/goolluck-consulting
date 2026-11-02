package com.seatlock.repository;

import com.seatlock.model.Booking;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Data-access layer for {@link Booking} entities.
 *
 * <p>The UNIQUE constraint on {@code seat_id} is enforced at the DDL level
 * (see {@code schema.sql}), providing the ultimate safeguard against
 * double-booking even if application-level checks fail.</p>
 */
@Repository
public interface BookingRepository extends JpaRepository<Booking, Long> {
}
