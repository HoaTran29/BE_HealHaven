package backend.healhaven.repository;

import backend.healhaven.entity.Workshop;
import backend.healhaven.enums.WorkshopStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.time.LocalDateTime;
import java.util.List;

@Repository
public interface WorkshopRepository extends JpaRepository<Workshop, Integer> {

    // Featured workshops
    List<Workshop> findByIsFeaturedTrueAndStatus(WorkshopStatus status);

    // Search by category
    Page<Workshop> findByCategoryAndStatus(String category, WorkshopStatus status, Pageable pageable);

    // Search by status
    Page<Workshop> findByStatus(WorkshopStatus status, Pageable pageable);

    // Advanced search with multiple filters
    @Query("SELECT w FROM Workshop w " +
            "LEFT JOIN w.venue v " +
            "WHERE w.status = :status " +
            "AND (:category IS NULL OR w.category = :category) " +
            "AND (:district IS NULL OR v.district = :district) " +
            "AND (:minPrice IS NULL OR w.price >= :minPrice) " +
            "AND (:maxPrice IS NULL OR w.price <= :maxPrice) " +
            "AND (:startDate IS NULL OR w.startTime >= :startDate) " +
            "AND (:keyword IS NULL OR LOWER(w.title) LIKE LOWER(CONCAT('%', :keyword, '%')))")
    Page<Workshop> searchWorkshops(
            @Param("status") WorkshopStatus status,
            @Param("category") String category,
            @Param("district") String district,
            @Param("minPrice") java.math.BigDecimal minPrice,
            @Param("maxPrice") java.math.BigDecimal maxPrice,
            @Param("startDate") LocalDateTime startDate,
            @Param("keyword") String keyword,
            Pageable pageable);

    // Find workshops by host
    List<Workshop> findByHostUserId(Integer hostId);

    // Count bookings for a workshop
    @Query("SELECT COUNT(b) FROM Booking b WHERE b.workshop.workshopId = :workshopId AND b.bookingStatus != 'PENDING'")
    int countConfirmedBookings(@Param("workshopId") Integer workshopId);
}
