package backend.healhaven.repository;

import backend.healhaven.entity.VenueBooking;
import backend.healhaven.enums.VenueBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDate;
import java.time.LocalTime;
import java.util.List;

@Repository
public interface VenueBookingRepository extends JpaRepository<VenueBooking, Integer> {
        List<VenueBooking> findByHostUserId(Integer hostId);

        List<VenueBooking> findByVenueVenueId(Integer venueId);

        List<VenueBooking> findByVenueVenueIdAndStatus(Integer venueId, VenueBookingStatus status);

        // Lấy booking theo venue và khoảng ngày (cho calendar)
        List<VenueBooking> findByVenueVenueIdAndBookingDateBetweenOrderByBookingDateAscStartTimeAsc(
                        Integer venueId, LocalDate from, LocalDate to);

        // Lấy tất cả booking của các venue thuộc provider
        @Query("SELECT vb FROM VenueBooking vb WHERE vb.venue.provider.userId = :providerId ORDER BY vb.bookingDate DESC")
        List<VenueBooking> findByVenueProviderUserId(@Param("providerId") Integer providerId);

        // Kiểm tra trùng lịch: có booking nào đã CONFIRMED trùng ngày + giờ không
        @Query("SELECT vb FROM VenueBooking vb WHERE vb.venue.venueId = :venueId " +
                        "AND vb.bookingDate = :bookingDate " +
                        "AND vb.startTime < :endTime AND vb.endTime > :startTime " +
                        "AND vb.status = 'CONFIRMED'")
        List<VenueBooking> findConflictingBookings(
                        @Param("venueId") Integer venueId,
                        @Param("bookingDate") LocalDate bookingDate,
                        @Param("startTime") LocalTime startTime,
                        @Param("endTime") LocalTime endTime);

        // Calculate total expense for a host
        @Query("SELECT COALESCE(SUM(vb.totalCost), 0) FROM VenueBooking vb " +
                        "WHERE vb.host.userId = :hostId")
        java.math.BigDecimal calculateTotalExpense(@Param("hostId") Integer hostId);
}
