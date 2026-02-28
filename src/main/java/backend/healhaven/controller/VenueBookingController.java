package backend.healhaven.controller;

import backend.healhaven.dto.request.VenueBookingRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.VenueBookingResponse;
import backend.healhaven.enums.VenueBookingStatus;
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
import org.springframework.web.bind.annotation.*;

import java.time.LocalDate;
import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/venue-bookings")
@RequiredArgsConstructor
@Tag(name = "Venue Bookings", description = "Venue Rental, Calendar & Approval APIs")
public class VenueBookingController {

    private final VenueBookingService venueBookingService;
    private final UserRepository userRepository;

    // ==================== HOST APIs ====================

    @PostMapping
    @Operation(summary = "Đặt venue", description = "Host tạo yêu cầu thuê venue")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> createBooking(
            @Valid @RequestBody VenueBookingRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserId(userDetails);
        VenueBookingResponse response = venueBookingService.createBooking(request, hostId);
        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Yêu cầu đặt venue thành công", response));
    }

    @GetMapping("/my-bookings")
    @Operation(summary = "Đơn đặt của tôi", description = "Host xem danh sách đơn đặt venue của mình")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getMyBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserId(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getMyBookings(hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== PROVIDER APPROVAL APIs ====================

    @GetMapping("/provider/all")
    @Operation(summary = "Tất cả đơn thuê", description = "Provider xem tất cả booking của các venue mình sở hữu")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getBookingsForProvider(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserId(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getBookingsForProvider(providerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/provider/pending")
    @Operation(summary = "Đơn chờ duyệt", description = "Provider xem các đơn thuê đang chờ phê duyệt")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getPendingBookings(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserId(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getBookingsForProviderByStatus(
                providerId, VenueBookingStatus.REQUESTING);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/provider/by-status")
    @Operation(summary = "Lọc theo trạng thái", description = "Provider lọc đơn thuê theo trạng thái: REQUESTING, CONFIRMED, REJECTED, CANCELLED")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getBookingsByStatus(
            @RequestParam VenueBookingStatus status,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserId(userDetails);
        List<VenueBookingResponse> response = venueBookingService.getBookingsForProviderByStatus(providerId, status);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/{bookingId}/approve")
    @Operation(summary = "Phê duyệt đơn thuê", description = "Provider phê duyệt đơn thuê venue từ Host")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> approveBooking(
            @PathVariable Integer bookingId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserId(userDetails);
        VenueBookingResponse response = venueBookingService.approveBooking(bookingId, providerId);
        return ResponseEntity.ok(ApiResponse.success("Đã phê duyệt đơn thuê venue", response));
    }

    @PutMapping("/{bookingId}/reject")
    @Operation(summary = "Từ chối đơn thuê", description = "Provider từ chối đơn thuê venue từ Host, kèm lý do")
    public ResponseEntity<ApiResponse<VenueBookingResponse>> rejectBooking(
            @PathVariable Integer bookingId,
            @RequestBody(required = false) Map<String, String> body,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer providerId = getUserId(userDetails);
        String reason = (body != null) ? body.get("reason") : null;
        VenueBookingResponse response = venueBookingService.rejectBooking(bookingId, providerId, reason);
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối đơn thuê venue", response));
    }

    // ==================== VENUE SCHEDULE APIs ====================

    @GetMapping("/venue/{venueId}")
    @Operation(summary = "Booking của venue", description = "Xem tất cả booking của 1 venue cụ thể")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getVenueBookings(
            @PathVariable Integer venueId) {
        List<VenueBookingResponse> response = venueBookingService.getVenueBookings(venueId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/venue/{venueId}/schedule")
    @Operation(summary = "Lịch trống venue", description = "Xem lịch booking theo khoảng ngày — khung giờ không có booking là trống")
    public ResponseEntity<ApiResponse<List<VenueBookingResponse>>> getVenueSchedule(
            @PathVariable Integer venueId,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate from,
            @RequestParam @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate to) {
        List<VenueBookingResponse> response = venueBookingService.getVenueSchedule(venueId, from, to);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // ==================== Helper ====================

    private Integer getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
