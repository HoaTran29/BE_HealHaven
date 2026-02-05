package backend.healhaven.repository;

import backend.healhaven.entity.Booking;
import backend.healhaven.enums.BookingStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;
import java.util.UUID;

@Repository
public interface BookingRepository extends JpaRepository<Booking, Integer> {

    // Find by attendee
    Page<Booking> findByAttendeeUserId(Integer attendeeId, Pageable pageable);

    // Find by attendee and status
    List<Booking> findByAttendeeUserIdAndBookingStatus(Integer attendeeId, BookingStatus status);

    // Find by checkin code
    Optional<Booking> findByCheckinCode(UUID checkinCode);

    // Find upcoming bookings for calendar (paid, not yet attended)
    @Query("SELECT b FROM Booking b " +
            "JOIN b.workshop w " +
            "WHERE b.attendee.userId = :userId " +
            "AND b.bookingStatus IN ('PAID', 'ATTENDED') " +
            "AND w.startTime >= :fromDate " +
            "ORDER BY w.startTime ASC")
    List<Booking> findUpcomingBookings(
            @Param("userId") Integer userId,
            @Param("fromDate") LocalDateTime fromDate);

    // Check if user already booked this workshop
    boolean existsByAttendeeUserIdAndWorkshopWorkshopIdAndBookingStatusNot(
            Integer attendeeId, Integer workshopId, BookingStatus status);

    // Count total bookings for workshop
    @Query("SELECT COALESCE(SUM(b.quantity), 0) FROM Booking b " +
            "WHERE b.workshop.workshopId = :workshopId " +
            "AND b.bookingStatus NOT IN ('PENDING')")
    int countTotalBookedSeats(@Param("workshopId") Integer workshopId);
}
