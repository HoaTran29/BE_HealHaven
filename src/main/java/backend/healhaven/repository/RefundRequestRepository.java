package backend.healhaven.repository;

import backend.healhaven.entity.RefundRequest;
import backend.healhaven.enums.RefundStatus;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RefundRequestRepository extends JpaRepository<RefundRequest, Integer> {

    // Find by booking
    Optional<RefundRequest> findByBookingBookingId(Integer bookingId);

    // Check if booking has pending refund
    boolean existsByBookingBookingIdAndStatus(Integer bookingId, RefundStatus status);

    // Find refunds by user
    Page<RefundRequest> findByBookingAttendeeUserId(Integer userId, Pageable pageable);
}
