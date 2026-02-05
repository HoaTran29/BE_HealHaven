package backend.healhaven.service;

import backend.healhaven.dto.request.CreateRefundRequest;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.RefundResponse;
import backend.healhaven.entity.Booking;
import backend.healhaven.entity.User;
import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.PaymentStatus;
import backend.healhaven.enums.RefundStatus;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.RefundRequestRepository;
import backend.healhaven.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.time.LocalDateTime;
import java.time.temporal.ChronoUnit;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class RefundService {

    private final RefundRequestRepository refundRequestRepository;
    private final BookingRepository bookingRepository;
    private final CustomUserDetailsService userDetailsService;

    // Refund policy: 20% fee if cancelled within 24 hours, 10% if more than 24
    // hours
    private static final BigDecimal FEE_WITHIN_24H = new BigDecimal("0.20");
    private static final BigDecimal FEE_MORE_THAN_24H = new BigDecimal("0.10");

    @Transactional
    public RefundResponse createRefundRequest(CreateRefundRequest request) {
        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        // Verify ownership
        if (!booking.getAttendee().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You don't have access to this booking");
        }

        // Validate booking status - must be PAID
        if (booking.getPaymentStatus() != PaymentStatus.PAID) {
            throw new BadRequestException("Only paid bookings can be refunded");
        }

        // Check if already has a refund request
        if (refundRequestRepository.existsByBookingBookingIdAndStatus(
                booking.getBookingId(), RefundStatus.PENDING)) {
            throw new BadRequestException("A refund request is already pending for this booking");
        }

        // Check if workshop already started
        if (booking.getWorkshop().getStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Cannot request refund after workshop has started");
        }

        // Calculate refund amount based on time to workshop
        BigDecimal refundAmount = calculateRefundAmount(booking);

        // Create refund request
        backend.healhaven.entity.RefundRequest refundRequest = backend.healhaven.entity.RefundRequest.builder()
                .booking(booking)
                .reason(request.getReason())
                .refundAmount(refundAmount)
                .status(RefundStatus.PENDING)
                .build();

        refundRequest = refundRequestRepository.save(refundRequest);

        return mapToResponse(refundRequest);
    }

    @Transactional(readOnly = true)
    public PageResponse<RefundResponse> getMyRefundRequests(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("requestedAt").descending());

        Page<backend.healhaven.entity.RefundRequest> refundPage = refundRequestRepository
                .findByBookingAttendeeUserId(currentUser.getUserId(), pageable);

        List<RefundResponse> content = refundPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<RefundResponse>builder()
                .content(content)
                .page(refundPage.getNumber())
                .size(refundPage.getSize())
                .totalElements(refundPage.getTotalElements())
                .totalPages(refundPage.getTotalPages())
                .first(refundPage.isFirst())
                .last(refundPage.isLast())
                .build();
    }

    private BigDecimal calculateRefundAmount(Booking booking) {
        BigDecimal originalAmount = booking.getTotalPrice();
        LocalDateTime workshopStart = booking.getWorkshop().getStartTime();
        long hoursUntilStart = ChronoUnit.HOURS.between(LocalDateTime.now(), workshopStart);

        BigDecimal fee;
        if (hoursUntilStart < 24) {
            fee = originalAmount.multiply(FEE_WITHIN_24H);
        } else {
            fee = originalAmount.multiply(FEE_MORE_THAN_24H);
        }

        return originalAmount.subtract(fee).setScale(2, RoundingMode.HALF_UP);
    }

    private RefundResponse mapToResponse(backend.healhaven.entity.RefundRequest refundRequest) {
        return RefundResponse.builder()
                .refundId(refundRequest.getRefundId())
                .bookingId(refundRequest.getBooking().getBookingId())
                .reason(refundRequest.getReason())
                .refundAmount(refundRequest.getRefundAmount())
                .status(refundRequest.getStatus())
                .requestedAt(refundRequest.getRequestedAt())
                .workshopTitle(refundRequest.getBooking().getWorkshop().getTitle())
                .originalAmount(refundRequest.getBooking().getTotalPrice())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDetailsService.getUserByEmail(email);
    }
}
