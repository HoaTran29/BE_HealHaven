package backend.healhaven.controller;

import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.BookingResponse;
import backend.healhaven.enums.PaymentStatus;
import backend.healhaven.service.BookingService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/payments")
@RequiredArgsConstructor
@Tag(name = "Payments", description = "Payment APIs (Mock)")
public class PaymentController {

    private final BookingService bookingService;

    @PostMapping("/mock/{bookingId}")
    @Operation(summary = "Mock Payment (Test Only)", description = "Simulate a successful payment for a booking")
    public ResponseEntity<ApiResponse<BookingResponse>> mockPayment(@PathVariable Integer bookingId) {
        BookingResponse response = bookingService.updatePaymentStatus(bookingId, PaymentStatus.PAID);
        return ResponseEntity.ok(ApiResponse.success("Payment simulated successfully", response));
    }
}
