package backend.healhaven.controller;

import backend.healhaven.dto.request.BookingRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.service.BookingService;
import backend.healhaven.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/bookings")
@RequiredArgsConstructor
@Tag(name = "Bookings", description = "Booking APIs")
@SecurityRequirement(name = "bearerAuth")
public class BookingController {

    private final BookingService bookingService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create booking", description = "Book a workshop")
    public ResponseEntity<ApiResponse<BookingResponse>> createBooking(
            @Valid @RequestBody BookingRequest request) {
        BookingResponse response = bookingService.createBooking(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Booking created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get my bookings", description = "Get list of user's bookings")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getMyBookings(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<BookingResponse> response = bookingService.getMyBookings(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{bookingId}")
    @Operation(summary = "Get booking details", description = "Get booking by ID")
    public ResponseEntity<ApiResponse<BookingResponse>> getBookingById(
            @PathVariable Integer bookingId) {
        BookingResponse response = bookingService.getBookingById(bookingId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/calendar")
    @Operation(summary = "Get upcoming bookings", description = "Get user's upcoming bookings for calendar")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getUpcomingBookings() {
        List<BookingResponse> response = bookingService.getUpcomingBookings();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/workshop/{workshopId}")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Get workshop attendees", description = "Get list of bookings for a specific workshop (HOST only)")
    public ResponseEntity<ApiResponse<List<BookingResponse>>> getWorkshopBookings(
            @PathVariable Integer workshopId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        List<BookingResponse> response = bookingService.getWorkshopBookings(workshopId, hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping("/check-in")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Check-in attendee", description = "Check-in an attendee using their code (HOST only)")
    public ResponseEntity<ApiResponse<BookingResponse>> checkInAttendee(
            @RequestBody java.util.Map<String, String> request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        String checkinCode = request.get("checkinCode");
        if (checkinCode == null) {
            throw new backend.healhaven.exception.BadRequestException("Check-in code is required");
        }
        Integer hostId = getUserIdFromUserDetails(userDetails);
        BookingResponse response = bookingService.checkInAttendee(checkinCode, hostId);
        return ResponseEntity.ok(ApiResponse.success("Attendee checked in successfully", response));
    }

    private Integer getUserIdFromUserDetails(org.springframework.security.core.userdetails.UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new backend.healhaven.exception.ResourceNotFoundException("User", "email",
                        userDetails.getUsername()))
                .getUserId();
    }
}
