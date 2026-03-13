package backend.healhaven.controller;

import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.service.ManualPaymentService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Manual Bank Transfer Payment APIs")
@SecurityRequirement(name = "bearerAuth")
public class PaymentController {

    private final ManualPaymentService manualPaymentService;

    /**
     * User xác nhận đã chuyển khoản ngân hàng.
     * FE tự hiển thị mã QR cố định → user chuyển tiền → bấm xác nhận → gọi API này.
     * Booking: PENDING → PENDING_CONFIRMATION
     * Admin sẽ nhận notification và kiểm tra tài khoản để approve/reject.
     */
    @PostMapping("/confirm/{bookingId}")
    @Operation(
            summary = "Xác nhận đã chuyển khoản",
            description = "User xác nhận đã chuyển khoản ngân hàng. Admin sẽ kiểm tra và duyệt thủ công. Booking chuyển sang trạng thái PENDING_CONFIRMATION."
    )
    public ResponseEntity<ApiResponse<BookingResponse>> confirmPayment(
            @PathVariable Integer bookingId) {
        BookingResponse response = manualPaymentService.confirmPayment(bookingId);
        return ResponseEntity.ok(ApiResponse.success(
                "Đã xác nhận chuyển khoản. Admin sẽ kiểm tra và xác nhận thanh toán cho bạn sớm nhất có thể.",
                response
        ));
    }
}
