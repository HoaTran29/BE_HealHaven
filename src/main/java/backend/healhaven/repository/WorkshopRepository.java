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
        // Using nativeQuery=true + CAST(:keyword AS text) to avoid PostgreSQL error:
        // "function lower(bytea) does not exist" when keyword param is null
        @Query(value = "SELECT w.* FROM workshops w " +
                        "LEFT JOIN venues v ON v.venue_id = w.venue_id " +
                        "WHERE w.status = CAST(:status AS text) " +
                        "AND (CAST(:category AS text) IS NULL OR w.category = CAST(:category AS text)) " +
                        "AND (CAST(:district AS text) IS NULL OR v.district = CAST(:district AS text)) " +
                        "AND (CAST(:minPrice AS numeric) IS NULL OR w.price >= CAST(:minPrice AS numeric)) " +
                        "AND (CAST(:maxPrice AS numeric) IS NULL OR w.price <= CAST(:maxPrice AS numeric)) " +
                        "AND (CAST(:startDate AS timestamp) IS NULL OR w.start_time >= CAST(:startDate AS timestamp)) "
                        +
                        "AND (CAST(:keyword AS text) IS NULL OR LOWER(w.title) LIKE LOWER('%' || CAST(:keyword AS text) || '%'))", countQuery = "SELECT COUNT(*) FROM workshops w "
                                        +
                                        "LEFT JOIN venues v ON v.venue_id = w.venue_id " +
                                        "WHERE w.status = CAST(:status AS text) " +
                                        "AND (CAST(:category AS text) IS NULL OR w.category = CAST(:category AS text)) "
                                        +
                                        "AND (CAST(:district AS text) IS NULL OR v.district = CAST(:district AS text)) "
                                        +
                                        "AND (CAST(:minPrice AS numeric) IS NULL OR w.price >= CAST(:minPrice AS numeric)) "
                                        +
                                        "AND (CAST(:maxPrice AS numeric) IS NULL OR w.price <= CAST(:maxPrice AS numeric)) "
                                        +
                                        "AND (CAST(:startDate AS timestamp) IS NULL OR w.start_time >= CAST(:startDate AS timestamp)) "
                                        +
                                        "AND (CAST(:keyword AS text) IS NULL OR LOWER(w.title) LIKE LOWER('%' || CAST(:keyword AS text) || '%'))", nativeQuery = true)
        Page<Workshop> searchWorkshops(
                        @Param("status") String status,
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
