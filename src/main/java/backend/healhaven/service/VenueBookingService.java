package backend.healhaven.service;

import backend.healhaven.dto.request.VenueBookingRequest;
import backend.healhaven.dto.response.VenueBookingResponse;
import backend.healhaven.entity.User;
import backend.healhaven.entity.Venue;
import backend.healhaven.entity.VenueBooking;
import backend.healhaven.enums.NotificationType;
import backend.healhaven.enums.VenueBookingStatus;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.repository.VenueBookingRepository;
import backend.healhaven.repository.VenueRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.time.Duration;
import java.time.LocalDate;
import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class VenueBookingService {

    private final VenueBookingRepository venueBookingRepository;
    private final VenueRepository venueRepository;
    private final UserRepository userRepository;
    private final NotificationService notificationService;

    // ==================== HOST APIs ====================

    @Transactional
    public VenueBookingResponse createBooking(VenueBookingRequest request, Integer hostId) {
        User host = userRepository.findById(hostId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", hostId));

        Venue venue = venueRepository.findById(request.getVenueId())
                .orElseThrow(() -> new ResourceNotFoundException("Venue", "id", request.getVenueId()));

        // Validate time
        if (request.getEndTime().isBefore(request.getStartTime())) {
            throw new BadRequestException("End time must be after start time");
        }

        // Kiểm tra trùng lịch với booking đã CONFIRMED
        List<VenueBooking> conflicts = venueBookingRepository.findConflictingBookings(
                request.getVenueId(), request.getBookingDate(),
                request.getStartTime(), request.getEndTime());
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Khung giờ này đã có booking được xác nhận, vui lòng chọn giờ khác");
        }

        // Calculate cost
        long hours = Duration.between(request.getStartTime(), request.getEndTime()).toHours();
        if (hours <= 0)
            hours = 1;
        BigDecimal totalCost = venue.getPricePerHour().multiply(BigDecimal.valueOf(hours));

        // Create booking
        VenueBooking booking = VenueBooking.builder()
                .host(host)
                .venue(venue)
                .bookingDate(request.getBookingDate())
                .startTime(request.getStartTime())
                .endTime(request.getEndTime())
                .totalCost(totalCost)
                .status(VenueBookingStatus.REQUESTING)
                .build();

        booking = venueBookingRepository.save(booking);

        // Thông báo cho Provider có đơn thuê mới
        if (venue.getProvider() != null) {
            notificationService.sendNotification(
                    venue.getProvider().getUserId(),
                    "Đơn thuê venue mới",
                    String.format("Host %s đã gửi yêu cầu thuê venue \"%s\" ngày %s (%s - %s)",
                            host.getFullName(), venue.getName(),
                            request.getBookingDate(), request.getStartTime(), request.getEndTime()),
                    NotificationType.NEW_BOOKING_REQUEST,
                    booking.getVBookingId());
        }

        return mapToResponse(booking);
    }

    public List<VenueBookingResponse> getMyBookings(Integer hostId) {
        return venueBookingRepository.findByHostUserId(hostId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== PROVIDER APIs ====================

    /**
     * Lấy tất cả booking của các venue thuộc provider
     */
    public List<VenueBookingResponse> getBookingsForProvider(Integer providerId) {
        return venueBookingRepository.findByVenueProviderUserId(providerId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy booking theo status (REQUESTING = chờ duyệt, CONFIRMED, REJECTED)
     */
    public List<VenueBookingResponse> getBookingsForProviderByStatus(Integer providerId, VenueBookingStatus status) {
        return venueBookingRepository.findByVenueProviderUserIdAndStatus(providerId, status).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Provider phê duyệt đơn thuê
     */
    @Transactional
    public VenueBookingResponse approveBooking(Integer bookingId, Integer providerId) {
        VenueBooking booking = venueBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("VenueBooking", "id", bookingId));

        // Verify provider owns the venue
        verifyProviderOwnership(booking, providerId);

        // Chỉ có thể duyệt đơn đang REQUESTING
        if (booking.getStatus() != VenueBookingStatus.REQUESTING) {
            throw new BadRequestException(
                    String.format("Không thể phê duyệt đơn có trạng thái %s. Chỉ có thể duyệt đơn đang REQUESTING.",
                            booking.getStatus()));
        }

        // Kiểm tra trùng lịch trước khi approve
        List<VenueBooking> conflicts = venueBookingRepository.findConflictingBookings(
                booking.getVenue().getVenueId(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime());
        // Loại bỏ chính booking này khỏi kết quả
        conflicts.removeIf(c -> c.getVBookingId().equals(bookingId));
        if (!conflicts.isEmpty()) {
            throw new BadRequestException("Khung giờ này đã có booking khác được xác nhận, không thể phê duyệt.");
        }

        booking.setStatus(VenueBookingStatus.CONFIRMED);
        booking = venueBookingRepository.save(booking);

        // Thông báo cho Host
        notificationService.sendNotification(
                booking.getHost().getUserId(),
                "Đơn thuê venue đã được phê duyệt ✅",
                String.format("Đơn thuê venue \"%s\" ngày %s (%s - %s) đã được chủ venue phê duyệt.",
                        booking.getVenue().getName(), booking.getBookingDate(),
                        booking.getStartTime(), booking.getEndTime()),
                NotificationType.BOOKING_APPROVED,
                booking.getVBookingId());

        return mapToResponse(booking);
    }

    /**
     * Provider từ chối đơn thuê
     */
    @Transactional
    public VenueBookingResponse rejectBooking(Integer bookingId, Integer providerId, String reason) {
        VenueBooking booking = venueBookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("VenueBooking", "id", bookingId));

        // Verify provider owns the venue
        verifyProviderOwnership(booking, providerId);

        // Chỉ có thể từ chối đơn đang REQUESTING
        if (booking.getStatus() != VenueBookingStatus.REQUESTING) {
            throw new BadRequestException(
                    String.format("Không thể từ chối đơn có trạng thái %s. Chỉ có thể từ chối đơn đang REQUESTING.",
                            booking.getStatus()));
        }

        booking.setStatus(VenueBookingStatus.REJECTED);
        booking.setRejectionReason(reason);
        booking = venueBookingRepository.save(booking);

        // Thông báo cho Host
        String message = String.format("Đơn thuê venue \"%s\" ngày %s (%s - %s) đã bị từ chối.",
                booking.getVenue().getName(), booking.getBookingDate(),
                booking.getStartTime(), booking.getEndTime());
        if (reason != null && !reason.isBlank()) {
            message += " Lý do: " + reason;
        }

        notificationService.sendNotification(
                booking.getHost().getUserId(),
                "Đơn thuê venue bị từ chối ❌",
                message,
                NotificationType.BOOKING_REJECTED,
                booking.getVBookingId());

        return mapToResponse(booking);
    }

    /**
     * Xem lịch booking của 1 venue cụ thể theo khoảng ngày
     */
    public List<VenueBookingResponse> getVenueSchedule(Integer venueId, LocalDate from, LocalDate to) {
        if (!venueRepository.existsById(venueId)) {
            throw new ResourceNotFoundException("Venue", "id", venueId);
        }
        if (to.isBefore(from)) {
            throw new BadRequestException("Ngày kết thúc phải sau ngày bắt đầu");
        }
        return venueBookingRepository
                .findByVenueVenueIdAndBookingDateBetweenOrderByBookingDateAscStartTimeAsc(venueId, from, to)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Xem tất cả booking của 1 venue
     */
    public List<VenueBookingResponse> getVenueBookings(Integer venueId) {
        return venueBookingRepository.findByVenueVenueId(venueId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    // ==================== Helpers ====================

    private void verifyProviderOwnership(VenueBooking booking, Integer providerId) {
        if (booking.getVenue() == null || booking.getVenue().getProvider() == null
                || !booking.getVenue().getProvider().getUserId().equals(providerId)) {
            throw new BadRequestException("Bạn không phải chủ sở hữu venue này, không có quyền thao tác.");
        }
    }

    private VenueBookingResponse mapToResponse(VenueBooking booking) {
        return VenueBookingResponse.builder()
                .bookingId(booking.getVBookingId())
                .venueId(booking.getVenue().getVenueId())
                .venueName(booking.getVenue().getName())
                .venueAddress(booking.getVenue().getAddress())
                .hostId(booking.getHost().getUserId())
                .hostName(booking.getHost().getFullName())
                .hostEmail(booking.getHost().getEmail())
                .hostPhone(booking.getHost().getPhoneNumber())
                .bookingDate(booking.getBookingDate())
                .startTime(booking.getStartTime())
                .endTime(booking.getEndTime())
                .totalCost(booking.getTotalCost())
                .status(booking.getStatus())
                .rejectionReason(booking.getRejectionReason())
                .updatedAt(booking.getUpdatedAt())
                .build();
    }
}
