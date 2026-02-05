package backend.healhaven.controller;

import backend.healhaven.dto.request.ReviewRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.ReviewResponse;
import backend.healhaven.service.ReviewService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/reviews")
@RequiredArgsConstructor
@Tag(name = "Reviews", description = "Review APIs")
public class ReviewController {

    private final ReviewService reviewService;

    @PostMapping
    @Operation(summary = "Submit review", description = "Submit a review for an attended workshop")
    @SecurityRequirement(name = "bearerAuth")
    public ResponseEntity<ApiResponse<ReviewResponse>> createReview(
            @Valid @RequestBody ReviewRequest request) {
        ReviewResponse response = reviewService.createReview(request);
        return ResponseEntity
                .status(HttpStatus.CREATED)
                .body(ApiResponse.success("Review submitted successfully", response));
    }

    @GetMapping("/workshop/{workshopId}")
    @Operation(summary = "Get workshop reviews", description = "Get reviews for a specific workshop")
    public ResponseEntity<ApiResponse<PageResponse<ReviewResponse>>> getWorkshopReviews(
            @PathVariable Integer workshopId,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size) {
        PageResponse<ReviewResponse> response = reviewService.getWorkshopReviews(workshopId, page, size);
        return ResponseEntity.ok(ApiResponse.success(response));
    }
}
