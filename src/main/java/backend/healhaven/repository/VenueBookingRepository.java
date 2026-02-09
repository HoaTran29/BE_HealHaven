package backend.healhaven.repository;

import backend.healhaven.entity.VenueBooking;
import backend.healhaven.enums.VenueBookingStatus;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface VenueBookingRepository extends JpaRepository<VenueBooking, Integer> {
    List<VenueBooking> findByHostUserId(Integer hostId);

    List<VenueBooking> findByVenueVenueId(Integer venueId);

    List<VenueBooking> findByVenueVenueIdAndStatus(Integer venueId, VenueBookingStatus status);

    // Calculate total expense for a host
    @org.springframework.data.jpa.repository.Query("SELECT COALESCE(SUM(vb.totalCost), 0) FROM VenueBooking vb " +
            "WHERE vb.host.userId = :hostId")
    java.math.BigDecimal calculateTotalExpense(
            @org.springframework.data.repository.query.Param("hostId") Integer hostId);
}
