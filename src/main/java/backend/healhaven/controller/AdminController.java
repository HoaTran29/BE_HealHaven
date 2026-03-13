package backend.healhaven.controller;

import backend.healhaven.dto.request.UserRoleRequest;
import backend.healhaven.dto.request.UserStatusRequest;
import backend.healhaven.dto.request.RejectReasonRequest;
import backend.healhaven.dto.response.AdminUserResponse;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.WorkshopResponse;
import backend.healhaven.dto.response.VenueResponse;
import backend.healhaven.dto.response.AdminStatsResponse;
import backend.healhaven.dto.response.RevenueChartResponse;
import backend.healhaven.dto.response.WithdrawalResponse;
import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.enums.UserRole;
import backend.healhaven.enums.WorkshopStatus;
import backend.healhaven.enums.WithdrawalStatus;
import backend.healhaven.service.AdminService;
import backend.healhaven.service.ManualPaymentService;
import java.util.List;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/admin")
@RequiredArgsConstructor
@Tag(name = "Admin Dashboard", description = "Admin Management APIs")
@PreAuthorize("hasRole('ADMIN')")
public class AdminController {

    private final AdminService adminService;
    private final ManualPaymentService manualPaymentService;

    @GetMapping("/users")
    @Operation(summary = "Get all users (Paginated & Filtered)")
    public ResponseEntity<ApiResponse<PageResponse<AdminUserResponse>>> getUsers(
            @RequestParam(required = false) UserRole role,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<AdminUserResponse> response = adminService.getUsers(role, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/users/{userId}/status")
    @Operation(summary = "Update user status (ACTIVE/INACTIVE)")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserStatus(
            @PathVariable Integer userId,
            @Valid @RequestBody UserStatusRequest request) {
        AdminUserResponse response = adminService.updateUserStatus(userId, request.getStatus());
        return ResponseEntity.ok(ApiResponse.success("User status updated successfully", response));
    }

    @PutMapping("/users/{userId}/role")
    @Operation(summary = "Update user role")
    public ResponseEntity<ApiResponse<AdminUserResponse>> updateUserRole(
            @PathVariable Integer userId,
            @Valid @RequestBody UserRoleRequest request) {
        AdminUserResponse response = adminService.updateUserRole(userId, request.getRole());
        return ResponseEntity.ok(ApiResponse.success("User role updated successfully", response));
    }

    // --- WORKSHOP APPROVAL ---

    @GetMapping("/workshops")
    @Operation(summary = "Get all workshops (Paginated & Filtered by Status)")
    public ResponseEntity<ApiResponse<PageResponse<WorkshopResponse>>> getWorkshops(
            @RequestParam(required = false) WorkshopStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<WorkshopResponse> response = adminService.getWorkshops(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/workshops/{id}/approve")
    @Operation(summary = "Approve a pending workshop")
    public ResponseEntity<ApiResponse<WorkshopResponse>> approveWorkshop(@PathVariable Integer id) {
        WorkshopResponse response = adminService.approveWorkshop(id);
        return ResponseEntity.ok(ApiResponse.success("Workshop approved successfully", response));
    }

    @PutMapping("/workshops/{id}/reject")
    @Operation(summary = "Reject a pending workshop")
    public ResponseEntity<ApiResponse<WorkshopResponse>> rejectWorkshop(
            @PathVariable Integer id,
            @Valid @RequestBody RejectReasonRequest request) {
        WorkshopResponse response = adminService.rejectWorkshop(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Workshop rejected successfully", response));
    }

    // --- VENUE APPROVAL ---

    @GetMapping("/venues")
    @Operation(summary = "Get all venues (Paginated & Filtered by Status)")
    public ResponseEntity<ApiResponse<PageResponse<VenueResponse>>> getVenues(
            @RequestParam(required = false) String status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<VenueResponse> response = adminService.getVenues(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/venues/{id}/approve")
    @Operation(summary = "Approve a pending venue")
    public ResponseEntity<ApiResponse<VenueResponse>> approveVenue(@PathVariable Integer id) {
        VenueResponse response = adminService.approveVenue(id);
        return ResponseEntity.ok(ApiResponse.success("Venue approved successfully", response));
    }

    @PutMapping("/venues/{id}/reject")
    @Operation(summary = "Reject a pending venue")
    public ResponseEntity<ApiResponse<VenueResponse>> rejectVenue(
            @PathVariable Integer id,
            @Valid @RequestBody RejectReasonRequest request) {
        VenueResponse response = adminService.rejectVenue(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Venue rejected successfully", response));
    }

    // --- DASHBOARD ANALYTICS ---

    @GetMapping("/stats/overview")
    @Operation(summary = "Get system overview stats (users, workshops, revenues)")
    public ResponseEntity<ApiResponse<AdminStatsResponse>> getOverviewStats() {
        AdminStatsResponse response = adminService.getOverviewStats();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/stats/revenue-chart")
    @Operation(summary = "Get revenue chart data (monthly)")
    public ResponseEntity<ApiResponse<List<RevenueChartResponse>>> getRevenueChart(
            @RequestParam(defaultValue = "monthly") String type,
            @RequestParam int year) {
        List<RevenueChartResponse> response = adminService.getRevenueChart(type, year);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    // --- WITHDRAWALS ---

    @GetMapping("/withdrawals")
    @Operation(summary = "Get all withdrawals (Paginated & Filtered by Status)")
    public ResponseEntity<ApiResponse<PageResponse<WithdrawalResponse>>> getWithdrawals(
            @RequestParam(required = false) WithdrawalStatus status,
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<WithdrawalResponse> response = adminService.getWithdrawals(status, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/withdrawals/{id}/complete")
    @Operation(summary = "Complete a pending withdrawal")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> completeWithdrawal(@PathVariable Integer id) {
        WithdrawalResponse response = adminService.completeWithdrawal(id);
        return ResponseEntity.ok(ApiResponse.success("Withdrawal completed successfully", response));
    }

    @PutMapping("/withdrawals/{id}/reject")
    @Operation(summary = "Reject a pending withdrawal")
    public ResponseEntity<ApiResponse<WithdrawalResponse>> rejectWithdrawal(
            @PathVariable Integer id,
            @Valid @RequestBody RejectReasonRequest request) {
        WithdrawalResponse response = adminService.rejectWithdrawal(id, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Withdrawal rejected successfully", response));
    }

    // --- PAYMENT CONFIRMATION ---

    @GetMapping("/payments/pending")
    @Operation(summary = "Danh sách booking chờ xác nhận thanh toán",
            description = "Lấy danh sách các booking user đã xác nhận chuyển khoản nhưng chưa được admin duyệt")
    public ResponseEntity<ApiResponse<PageResponse<BookingResponse>>> getPendingPayments(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size) {
        PageResponse<BookingResponse> response = manualPaymentService.getPendingConfirmationBookings(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PutMapping("/payments/{bookingId}/approve")
    @Operation(summary = "Duyệt thanh toán",
            description = "Xác nhận đã nhận được tiền chuyển khoản, đổi booking status thành PAID")
    public ResponseEntity<ApiResponse<BookingResponse>> approvePayment(
            @PathVariable Integer bookingId) {
        BookingResponse response = manualPaymentService.approvePayment(bookingId);
        return ResponseEntity.ok(ApiResponse.success("Đã xác nhận thanh toán thành công", response));
    }

    @PutMapping("/payments/{bookingId}/reject")
    @Operation(summary = "Từ chối xác nhận thanh toán",
            description = "Từ chối booking (chưa thấy tiền), booking trở về PENDING để user thử lại")
    public ResponseEntity<ApiResponse<BookingResponse>> rejectPayment(
            @PathVariable Integer bookingId,
            @Valid @RequestBody RejectReasonRequest request) {
        BookingResponse response = manualPaymentService.rejectPayment(bookingId, request.getReason());
        return ResponseEntity.ok(ApiResponse.success("Đã từ chối xác nhận thanh toán", response));
    }
}
