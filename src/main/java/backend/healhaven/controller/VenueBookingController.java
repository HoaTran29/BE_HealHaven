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
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;

@RestController
@RequestMapping("/venue-bookings")
@RequiredArgsConstructor
@Tag(name = "Venue Bookings", description = "Venue Rental & Calendar APIs")
public class VenueBookingController {

    private final VenueBookingService venueBookingService;
    private final UserRepository userRepository;

    // ==================== HOST APIs ====================

    @PostMapping
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Đặt venue", description = "Tạo yêu cầu đặt venue (chỉ HOST)")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> createBooking(
            @Valid @RequestBody VenueBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        VenueBookingResponse response = venueBookingService.createBooking(request, hostId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Yêu cầu đặt venue thành công", response));
    }

    @GetMapping("/my-bookings")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Xem đơn đặt của tôi", description = "Lấy danh sách đơn đặt venue của host hiện tại")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getMyBookings(hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== PROVIDER APIs (KAN-27) ====================

    @GetMapping("/provider/all")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Xem tất cả đơn thuê", description = "Provider xem tất cả booking của các venue mình sở hữu")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getBookingsForProvider(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserIdFromUserDetails(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getBookingsForProvider(providerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venue/{venueId}")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Xem booking của venue", description = "Xem tất cả booking của 1 venue cụ thể")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getVenueBookings(
            @PathVariable Integer venueId) {
        List<VenueBookingResponse> response = venueBookingService.getVenueBookings(venueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venue/{venueId}/schedule")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Xem lịch trống venue", description = "Xem lịch booking theo khoảng ngày — khung giờ không có booking là trống")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getVenueSchedule(
            @PathVariable Integer venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<VenueBookingResponse> response = venueBookingService.getVenueSchedule(venueId, from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Helper ====================

    private Integer getUserIdFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
