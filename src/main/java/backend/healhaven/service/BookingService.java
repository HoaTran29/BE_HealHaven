package backend.healhaven.service;

import backend.healhaven.dto.request.BookingRequest;
import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.entity.Booking;
import backend.healhaven.entity.Media;
import backend.healhaven.entity.User;
import backend.healhaven.entity.Workshop;
import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.PaymentStatus;
import backend.healhaven.enums.WorkshopStatus;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.MediaRepository;
import backend.healhaven.repository.ReviewRepository;
import backend.healhaven.repository.WorkshopRepository;
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
import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class BookingService {

    private final BookingRepository bookingRepository;
    private final WorkshopRepository workshopRepository;
    private final ReviewRepository reviewRepository;
    private final MediaRepository mediaRepository;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public BookingResponse createBooking(BookingRequest request) {
        User currentUser = getCurrentUser();

        Workshop workshop = workshopRepository.findById(request.getWorkshopId())
                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", request.getWorkshopId()));

        // Validate workshop status
        if (workshop.getStatus() != WorkshopStatus.PUBLISHED) {
            throw new BadRequestException("Workshop is not available for booking");
        }

        // Check if workshop already started
        if (workshop.getStartTime().isBefore(LocalDateTime.now())) {
            throw new BadRequestException("Workshop has already started");
        }

        // Check available seats
        int bookedSeats = bookingRepository.countTotalBookedSeats(workshop.getWorkshopId());
        int availableSeats = workshop.getMaxAttendees() - bookedSeats;

        if (request.getQuantity() > availableSeats) {
            throw new BadRequestException("Not enough seats available. Only " + availableSeats + " left.");
        }

        // Check if user already booked this workshop
        boolean alreadyBooked = bookingRepository.existsByAttendeeUserIdAndWorkshopWorkshopIdAndBookingStatusNot(
                currentUser.getUserId(), workshop.getWorkshopId(), BookingStatus.PENDING);
        if (alreadyBooked) {
            throw new BadRequestException("You have already booked this workshop");
        }

        // Calculate total price
        BigDecimal totalPrice = workshop.getPrice().multiply(BigDecimal.valueOf(request.getQuantity()));

        // Create booking
        Booking booking = Booking.builder()
                .attendee(currentUser)
                .workshop(workshop)
                .quantity(request.getQuantity())
                .totalPrice(totalPrice)
                .paymentStatus(PaymentStatus.PENDING)
                .bookingStatus(BookingStatus.PENDING)
                .build();

        booking = bookingRepository.save(booking);

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public PageResponse<BookingResponse> getMyBookings(int page, int size) {
        User currentUser = getCurrentUser();
        Pageable pageable = PageRequest.of(page, size, Sort.by("createdAt").descending());

        Page<Booking> bookingPage = bookingRepository.findByAttendeeUserId(currentUser.getUserId(), pageable);

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

    @Transactional(readOnly = true)
    public BookingResponse getBookingById(Integer bookingId) {
        User currentUser = getCurrentUser();
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        // Verify ownership
        if (!booking.getAttendee().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You don't have access to this booking");
        }

        return mapToResponse(booking);
    }

    @Transactional(readOnly = true)
    public List<BookingResponse> getUpcomingBookings() {
        User currentUser = getCurrentUser();

        List<Booking> bookings = bookingRepository.findUpcomingBookings(
                currentUser.getUserId(), LocalDateTime.now());

        return bookings.stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse updatePaymentStatus(Integer bookingId, PaymentStatus newStatus) {
        Booking booking = bookingRepository.findById(bookingId)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", bookingId));

        booking.setPaymentStatus(newStatus);

        // Update booking status based on payment
        if (newStatus == PaymentStatus.PAID) {
            booking.setBookingStatus(BookingStatus.PAID);
        }

        booking = bookingRepository.save(booking);
        return mapToResponse(booking);
    }

    private BookingResponse mapToResponse(Booking booking) {
        Workshop workshop = booking.getWorkshop();

        // Get primary image
        String primaryImage = mediaRepository.findByTargetIdAndTargetTypeAndIsPrimaryTrue(
                workshop.getWorkshopId(), "WORKSHOP")
                .stream()
                .findFirst()
                .map(Media::getCloudUrl)
                .orElse(null);

        // Check if reviewed
        boolean hasReview = reviewRepository.existsByBookingBookingId(booking.getBookingId());

        // Check refund status
        String refundStatus = null;
        boolean hasRefundRequest = false;
        if (booking.getRefundRequest() != null) {
            hasRefundRequest = true;
            refundStatus = booking.getRefundRequest().getStatus().name();
        }

        BookingResponse.WorkshopSummary workshopSummary = BookingResponse.WorkshopSummary.builder()
                .workshopId(workshop.getWorkshopId())
                .title(workshop.getTitle())
                .category(workshop.getCategory())
                .primaryImage(primaryImage)
                .startTime(workshop.getStartTime())
                .endTime(workshop.getEndTime())
                .venueName(workshop.getVenue() != null ? workshop.getVenue().getName() : null)
                .venueAddress(workshop.getVenue() != null ? workshop.getVenue().getAddress() : null)
                .hostName(workshop.getHost() != null ? workshop.getHost().getFullName() : null)
                .build();

        return BookingResponse.builder()
                .bookingId(booking.getBookingId())
                .quantity(booking.getQuantity())
                .totalPrice(booking.getTotalPrice())
                .paymentStatus(booking.getPaymentStatus())
                .bookingStatus(booking.getBookingStatus())
                .checkinCode(booking.getCheckinCode())
                .checkinAt(booking.getCheckinAt())
                .createdAt(booking.getCreatedAt())
                .workshop(workshopSummary)
                .hasReview(hasReview)
                .hasRefundRequest(hasRefundRequest)
                .refundStatus(refundStatus)
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDetailsService.getUserByEmail(email);
    }

    public List<BookingResponse> getWorkshopBookings(Integer workshopId, Integer hostId) {
        Workshop workshop = workshopRepository.findById(workshopId)
                .orElseThrow(() -> new ResourceNotFoundException("Workshop", "id", workshopId));

        // Verify host ownership
        if (!workshop.getHost().getUserId().equals(hostId)) {
            throw new backend.healhaven.exception.BadRequestException("You are not the host of this workshop");
        }

        return bookingRepository.findByWorkshopWorkshopId(workshopId).stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    @Transactional
    public BookingResponse checkInAttendee(String checkinCode, Integer hostId) {
        UUID code;
        try {
            code = UUID.fromString(checkinCode);
        } catch (IllegalArgumentException e) {
            throw new backend.healhaven.exception.BadRequestException("Invalid check-in code format");
        }

        Booking booking = bookingRepository.findByCheckinCode(code)
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "checkinCode", checkinCode));

        // Verify host ownership
        if (!booking.getWorkshop().getHost().getUserId().equals(hostId)) {
            throw new backend.healhaven.exception.BadRequestException("You are not the host of this workshop");
        }

        if (booking.getBookingStatus() == BookingStatus.ATTENDED) {
            throw new backend.healhaven.exception.BadRequestException("Attendee already checked in");
        }

        booking.setBookingStatus(BookingStatus.ATTENDED);
        booking.setCheckinAt(java.time.LocalDateTime.now());

        return mapToResponse(bookingRepository.save(booking));
    }
}
