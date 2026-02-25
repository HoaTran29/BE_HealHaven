package backend.healhaven.controller;

import backend.healhaven.dto.request.VenueBookingRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.VenueBookingResponse;
import backend.healhaven.service.VenueBookingService;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venue-bookings")
@RequiredArgsConstructor
@Tag(name = "Venue Bookings", description = "Venue Rental APIs")
public class VenueBookingController {

    private final VenueBookingService venueBookingService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Book a venue", description = "Create a request to book a venue (HOST only)")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> createBooking(
            @Valid @RequestBody VenueBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        VenueBookingResponse response = venueBookingService.createBooking(request, hostId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue booking requested successfully", response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Get my venue bookings", description = "Get list of venue bookings made by current host (HOST only)")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getMyBookings(hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Integer getUserIdFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
