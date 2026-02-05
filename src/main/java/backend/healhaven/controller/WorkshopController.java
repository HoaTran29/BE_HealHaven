package backend.healhaven.controller;

import backend.healhaven.dto.request.WorkshopSearchRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.PageResponse;
import backend.healhaven.dto.response.WorkshopResponse;
import backend.healhaven.service.WorkshopService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@RestController
@RequestMapping("/workshops")
@RequiredArgsConstructor
@Tag(name = "Workshops", description = "Workshop APIs")
public class WorkshopController {

    private final WorkshopService workshopService;
    private final backend.healhaven.repository.UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Search workshops", description = "Search and filter workshops")
    public ResponseEntity<ApiResponse<PageResponse<WorkshopResponse>>> searchWorkshops(
            @RequestParam(required = false) String keyword,
            @RequestParam(required = false) String category,
            @RequestParam(required = false) String district,
            @RequestParam(required = false) BigDecimal minPrice,
            @RequestParam(required = false) BigDecimal maxPrice,
            @RequestParam(required = false) LocalDateTime startDate,
            @RequestParam(defaultValue = "0") Integer page,
            @RequestParam(defaultValue = "10") Integer size,
            @RequestParam(defaultValue = "startTime") String sortBy,
            @RequestParam(defaultValue = "asc") String sortDir) {

        WorkshopSearchRequest request = new WorkshopSearchRequest();
        request.setKeyword(keyword);
        request.setCategory(category);
        request.setDistrict(district);
        request.setMinPrice(minPrice);
        request.setMaxPrice(maxPrice);
        request.setStartDate(startDate);
        request.setPage(page);
        request.setSize(size);
        request.setSortBy(sortBy);
        request.setSortDir(sortDir);

        PageResponse<WorkshopResponse> response = workshopService.searchWorkshops(request);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{workshopId}")
    @Operation(summary = "Get workshop details", description = "Get detailed workshop information")
    public ResponseEntity<ApiResponse<WorkshopResponse>> getWorkshopById(
            @PathVariable Integer workshopId) {
        WorkshopResponse response = workshopService.getWorkshopById(workshopId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/featured")
    @Operation(summary = "Get featured workshops", description = "Get list of featured workshops")
    public ResponseEntity<ApiResponse<List<WorkshopResponse>>> getFeaturedWorkshops() {
        List<WorkshopResponse> response = workshopService.getFeaturedWorkshops();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @PostMapping
    @Operation(summary = "Create workshop", description = "Create a new workshop (HOST only)")
    public ResponseEntity<ApiResponse<WorkshopResponse>> createWorkshop(
            @RequestBody @jakarta.validation.Valid backend.healhaven.dto.request.WorkshopRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        WorkshopResponse response = workshopService.createWorkshop(request, hostId);
        return ResponseEntity.status(org.springframework.http.HttpStatus.CREATED)
                .body(ApiResponse.success("Workshop created successfully", response));
    }

    @PutMapping("/{workshopId}")
    @Operation(summary = "Update workshop", description = "Update an existing workshop (Owner only)")
    public ResponseEntity<ApiResponse<WorkshopResponse>> updateWorkshop(
            @PathVariable Integer workshopId,
            @RequestBody @jakarta.validation.Valid backend.healhaven.dto.request.WorkshopRequest request,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        WorkshopResponse response = workshopService.updateWorkshop(workshopId, request, hostId);
        return ResponseEntity.ok(ApiResponse.success("Workshop updated successfully", response));
    }

    @DeleteMapping("/{workshopId}")
    @Operation(summary = "Delete workshop", description = "Delete a workshop (Owner only, no confirmed bookings)")
    public ResponseEntity<ApiResponse<Void>> deleteWorkshop(
            @PathVariable Integer workshopId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        workshopService.deleteWorkshop(workshopId, hostId);
        return ResponseEntity.ok(ApiResponse.success("Workshop deleted successfully", null));
    }

    @PostMapping("/{workshopId}/publish")
    @Operation(summary = "Publish workshop", description = "Publish a draft workshop (Owner only)")
    public ResponseEntity<ApiResponse<WorkshopResponse>> publishWorkshop(
            @PathVariable Integer workshopId,
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        WorkshopResponse response = workshopService.publishWorkshop(workshopId, hostId);
        return ResponseEntity.ok(ApiResponse.success("Workshop published successfully", response));
    }

    @GetMapping("/my-workshops")
    @Operation(summary = "Get my workshops", description = "Get all workshops created by the current user")
    public ResponseEntity<ApiResponse<List<WorkshopResponse>>> getMyWorkshops(
            @org.springframework.security.core.annotation.AuthenticationPrincipal org.springframework.security.core.userdetails.UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        List<WorkshopResponse> response = workshopService.getMyWorkshops(hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Integer getUserIdFromUserDetails(org.springframework.security.core.userdetails.UserDetails userDetails) {
        // Get user from database by email
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new backend.healhaven.exception.ResourceNotFoundException("User", "email",
                        userDetails.getUsername()))
                .getUserId();
    }
}
