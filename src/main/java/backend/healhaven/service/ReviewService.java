package backend.healhaven.service;

import backend.healhaven.dto.request.ReviewRequest;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.ReviewResponse;
import backend.healhaven.entity.Booking;
import backend.healhaven.entity.Review;
import backend.healhaven.entity.User;
import backend.healhaven.enums.BookingStatus;
import backend.healhaven.exception.BadRequestException;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.BookingRepository;
import backend.healhaven.repository.ReviewRepository;
import backend.healhaven.security.CustomUserDetailsService;
import lombok.RequiredArgsConstructor;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class ReviewService {

    private final ReviewRepository reviewRepository;
    private final BookingRepository bookingRepository;
    private final CustomUserDetailsService userDetailsService;

    @Transactional
    public ReviewResponse createReview(ReviewRequest request) {
        User currentUser = getCurrentUser();

        Booking booking = bookingRepository.findById(request.getBookingId())
                .orElseThrow(() -> new ResourceNotFoundException("Booking", "id", request.getBookingId()));

        // Verify ownership
        if (!booking.getAttendee().getUserId().equals(currentUser.getUserId())) {
            throw new BadRequestException("You can only review your own bookings");
        }

        // Validate booking status - must be ATTENDED or COMPLETED
        if (booking.getBookingStatus() != BookingStatus.ATTENDED &&
                booking.getBookingStatus() != BookingStatus.COMPLETED) {
            throw new BadRequestException("You can only review after attending the workshop");
        }

        // Check if already reviewed
        if (reviewRepository.existsByBookingBookingId(booking.getBookingId())) {
            throw new BadRequestException("You have already reviewed this booking");
        }

        // Create review
        Review review = Review.builder()
                .booking(booking)
                .rating(request.getRating())
                .comment(request.getComment())
                .imageUrl(request.getImageUrl())
                .build();

        review = reviewRepository.save(review);

        // Update booking status to REVIEWED
        booking.setBookingStatus(BookingStatus.REVIEWED);
        bookingRepository.save(booking);

        return mapToResponse(review, currentUser);
    }

    @Transactional(readOnly = true)
    public PageResponse<ReviewResponse> getWorkshopReviews(Integer workshopId, int page, int size) {
        Pageable pageable = PageRequest.of(page, size);

        Page<Review> reviewPage = reviewRepository.findByWorkshopId(workshopId, pageable);

        List<ReviewResponse> content = reviewPage.getContent().stream()
                .map(review -> mapToResponse(review, review.getBooking().getAttendee()))
                .collect(Collectors.toList());

        return PageResponse.<ReviewResponse>builder()
                .content(content)
                .page(reviewPage.getNumber())
                .size(reviewPage.getSize())
                .totalElements(reviewPage.getTotalElements())
                .totalPages(reviewPage.getTotalPages())
                .first(reviewPage.isFirst())
                .last(reviewPage.isLast())
                .build();
    }

    private ReviewResponse mapToResponse(Review review, User reviewer) {
        return ReviewResponse.builder()
                .reviewId(review.getReviewId())
                .rating(review.getRating())
                .comment(review.getComment())
                .imageUrl(review.getImageUrl())
                .createdAt(review.getCreatedAt())
                .reviewer(ReviewResponse.ReviewerInfo.builder()
                        .fullName(reviewer.getFullName())
                        .avatarUrl(reviewer.getAvatarUrl())
                        .build())
                .build();
    }

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userDetailsService.getUserByEmail(email);
    }
}
