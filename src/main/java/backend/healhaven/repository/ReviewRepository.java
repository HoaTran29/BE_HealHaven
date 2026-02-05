package backend.healhaven.repository;

import backend.healhaven.entity.Review;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface ReviewRepository extends JpaRepository<Review, Integer> {

    // Find review by booking
    Optional<Review> findByBookingBookingId(Integer bookingId);

    // Check if booking already reviewed
    boolean existsByBookingBookingId(Integer bookingId);

    // Find reviews for a workshop
    @Query("SELECT r FROM Review r " +
            "JOIN r.booking b " +
            "WHERE b.workshop.workshopId = :workshopId " +
            "ORDER BY r.createdAt DESC")
    Page<Review> findByWorkshopId(@Param("workshopId") Integer workshopId, Pageable pageable);

    // Calculate average rating for workshop
    @Query("SELECT COALESCE(AVG(r.rating), 0) FROM Review r " +
            "JOIN r.booking b " +
            "WHERE b.workshop.workshopId = :workshopId")
    Double calculateAverageRating(@Param("workshopId") Integer workshopId);

    // Count reviews for workshop
    @Query("SELECT COUNT(r) FROM Review r " +
            "JOIN r.booking b " +
            "WHERE b.workshop.workshopId = :workshopId")
    int countByWorkshopId(@Param("workshopId") Integer workshopId);
}
