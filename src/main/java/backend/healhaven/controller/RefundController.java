package backend.healhaven.controller;

import backend.healhaven.dto.request.CreateRefundRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.RefundResponse;
import backend.healhaven.service.RefundService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/refunds")
@RequiredArgsConstructor
@Tag(name = "Refunds", description = "Refund APIs")
@SecurityRequirement(name = "bearerAuth")
public class RefundController {

    private final RefundService refundService;

    @PostMapping
    @Operation(summary = "Request refund", description = "Submit a refund request for a booking")
    public ResponseEntity<ApiResponse<RefundResponse>> createRefundRequest(
            @Valid @RequestBody CreateRefundRequest request) {
        RefundResponse response = refundService.createRefundRequest(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Refund request submitted successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get my refund requests", description = "Get list of user's refund requests")
    public ResponseEntity<ApiResponse<PageResponse<RefundResponse>>> getMyRefundRequests(
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<RefundResponse> response = refundService.getMyRefundRequests(page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
