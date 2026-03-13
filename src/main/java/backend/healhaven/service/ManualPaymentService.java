package backend.healhaven.service;

import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.entity.Booking;
import backend.healhaven.entity.User;
import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.NotificationType;
import backend.healhaven.enums.PaymentStatus;
import backend.healhaven.enums.UserRole;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.MediaRepository;
import backend.healhaven.repository.ReviewRepository;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
@RequiredArgsConstructor
public class ManualPaymentService {

    private final BookingRepository bookingRepository;
    private final UserRepository userRepository;
    private final ReviewRepository reviewRepository;
    private final MediaRepository mediaRepository;
    private final NotificationService notificationService;
    private final CustomUserDetailsService userDetailsService;

    /**
     * User xác nhận đã chuyển khoản.
     * Booking phải đang ở trạng thái PENDING.
     * Sau khi xác nhận: PENDING → PENDING_CONFIRMATION + gửi notification cho tất cả Admin.
     */
    @Transactional
    public BookingResponse confirmPayment(Integer bookingId) {
        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Kiểm tra quyền sở hữu
        if (!booking.getAttendee().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("Bạn không có quyền thực hiện thao tác này");
        }

        // Chỉ booking đang PENDING mới được confirm
        if (booking.getPaymentStatus() != PaymentStatus.PENDING) {
            throw new BadRequestException(
                    "Chỉ có thể xác nhận thanh toán cho booking đang chờ thanh toán. Trạng thái hiện tại: "
                            + booking.getPaymentStatus());
        }

        booking.setPaymentStatus(PaymentStatus.PENDING_CONFIRMATION);
        booking = bookingRepository.save(booking);

        // Gửi notification cho tất cả Admin
        notifyAllAdmins(
                "Có thanh toán chờ xác nhận",
                "Booking #" + bookingId + " của user " + currentUser.getFullName()
                        + " cần được xác nhận thanh toán.",
                NotificationType.PAYMENT_PENDING_CONFIRMATION,
                bookingId
        );

        log.info("User {} confirmed payment for booking {}", currentUser.getEmail(), bookingId);
        return mapToResponse(booking);
    }

    /**
     * Admin duyệt thanh toán.
     * PENDING_CONFIRMATION → PAID, booking_status → PAID.
     * Gửi notification cho User.
     */
    @Transactional
    public BookingResponse approvePayment(Integer bookingId) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getPaymentStatus() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException(
                    "Chỉ có thể duyệt booking ở trạng thái PENDING_CONFIRMATION. Trạng thái hiện tại: "
                            + booking.getPaymentStatus());
        }

        booking.setPaymentStatus(PaymentStatus.PAID);
        booking.setBookingStatus(BookingStatus.PAID);
        booking = bookingRepository.save(booking);

        // Gửi notification cho User
        notificationService.sendNotification(
                booking.getAttendee().getUserId(),
                "Thanh toán đã được xác nhận!",
                "Booking #" + bookingId + " (" + booking.getWorkshop().getTitle()
                        + ") đã được xác nhận thanh toán thành công. Chúc bạn tham gia vui vẻ!",
                NotificationType.PAYMENT_APPROVED,
                bookingId
        );

        log.info("Admin approved payment for booking {}", bookingId);
        return mapToResponse(booking);
    }

    /**
     * Admin từ chối thanh toán.
     * PENDING_CONFIRMATION → PENDING (user có thể thử lại).
     * Gửi notification cho User kèm lý do.
     */
    @Transactional
    public BookingResponse rejectPayment(Integer bookingId, String note) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        if (booking.getPaymentStatus() != PaymentStatus.PENDING_CONFIRMATION) {
            throw new BadRequestException(
                    "Chỉ có thể từ chối booking ở trạng thái PENDING_CONFIRMATION. Trạng thái hiện tại: "
                            + booking.getPaymentStatus());
        }

        booking.setPaymentStatus(PaymentStatus.PENDING);
        booking = bookingRepository.save(booking);

        // Gửi notification cho User kèm lý do
        String message = "Xác nhận thanh toán cho Booking #" + bookingId + " (" + booking.getWorkshop().getTitle()
                + ") đã bị từ chối.";
        if (note != null && !note.isBlank()) {
            message += " Lý do: " + note;
        }
        message += " Vui lòng kiểm tra lại và thử xác nhận lại.";

        notificationService.sendNotification(
                booking.getAttendee().getUserId(),
                "Xác nhận thanh toán bị từ chối",
                message,
                NotificationType.PAYMENT_REJECTED,
                bookingId
        );

        log.info("Admin rejected payment for booking {}. Reason: {}", bookingId, note);
        return mapToResponse(booking);
    }

    /**
     * Admin lấy danh sách booking đang chờ xác nhận thanh toán.
     */
    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getPendingConfirmationBookings(int page, int size) {
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());
        Page<Booking> bookingPage = bookingRepository.findByPaymentStatus(
                PaymentStatus.PENDING_CONFIRMATION, pageable);

        List<BookingResponse> content = bookingPage.getContent().stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());

        return PageResponse.<BookingResponse>builder()
                .content(content)
                .page(bookingPage.getNumber())
                .size(bookingPage.getSize())
                .totalElements(bookingPage.getTotalElements())
                .totalPages(bookingPage.getTotalPages())
                .first(bookingPage.isFirst())
                .last(bookingPage.isLast())
                .build();
    }

    // ─── Private helpers ─────────────────────────────────────────────────────

    private void notifyAllAdmins(String title, String message, NotificationType type, Integer referenceId) {
        List<User> admins = userRepository.findByRole(UserRole.ADMIN);
        for (User admin : admins) {
            notificationService.sendNotification(admin.getUserId(), title, message, type, referenceId);
        }
        if (admins.isEmpty()) {
            log.warn("No ADMIN users found to notify for booking {}", referenceId);
        }
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDetailsService.getUserByEmail(email);
    }

    private BookingResponse mapToResponse(Booking booking) {
        var workshop = booking.getWorkshop();

        String primaryImage = mediaRepository.findByTargetIdAndTargetTypeAndIsPrimaryTrue(
                workshop.getWorkshopId(), "WORKSHOP")
                .stream()
                .findFirst()
                .map(backend.healhaven.entity.Media::getCloudUrl)
                .orElse(null);

        boolean hasReview = reviewRepository.existsByBookingBookingId(booking.getBookingId());

        String refundStatus = null;
        boolean hasRefundRequest = false;
        if (booking.getRefundRequest() != null) {
            hasRefundRequest = true;
            refundStatus = booking.getRefundRequest().getStatus().name();
        }

        String finalVenueName = workshop.getVenue() != null ? workshop.getVenue().getName() : null;
        if (workshop.getDistrict() != null && !workshop.getDistrict().isBlank()) {
            finalVenueName = workshop.getDistrict();
        }

        String finalVenueAddress = workshop.getVenue() != null ? workshop.getVenue().getAddress() : null;
        if (workshop.getAddress() != null && !workshop.getAddress().isBlank()) {
            finalVenueAddress = workshop.getAddress();
        }

        BookingResponse.WorkshopSummary workshopSummary = BookingResponse.WorkshopSummary.builder()
                .workshopId(workshop.getWorkshopId())
                .title(workshop.getTitle())
                .category(workshop.getCategory())
                .primaryImage(primaryImage)
                .startTime(workshop.getStartTime())
                .endTime(workshop.getEndTime())
                .venueName(finalVenueName)
                .venueAddress(finalVenueAddress)
                .hostName(workshop.getHost() != null ? workshop.getHost().getFullName() : null)
                .build();

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .id(booking.getBookingId())
                .quantity(booking.getQuantity())
                .totalPrice(booking.getTotalPrice())
                .amount(booking.getTotalPrice())
                .paymentStatus(booking.getPaymentStatus())
                .bookingStatus(booking.getBookingStatus())
                .userId(booking.getAttendee() != null ? booking.getAttendee().getUserId() : null)
                .userName(booking.getAttendee() != null ? booking.getAttendee().getFullName() : null)
                .checkinCode(booking.getCheckinCode())
                .checkinAt(booking.getCheckinAt())
                .createdAt(booking.getCreatedAt())
                .workshop(workshopSummary)
                .hasReview(hasReview)
                .hasRefundRequest(hasRefundRequest)
                .refundStatus(refundStatus)
                .build();
    }
}
