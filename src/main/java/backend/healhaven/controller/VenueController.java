package backend.healhaven.controller;

import backend.healhaven.dto.request.VenueRequest;
import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.VenueResponse;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.service.VenueService;
import backend.healhaven.exception.ResourceNotFoundException;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Venue Management APIs - Quản lý địa điểm cho thuê")
public class VenueController {

    private final VenueService venueService;
    private final UserRepository userRepository;

    @PostMapping
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Tạo venue mới", description = "Đăng tải không gian cho thuê (chỉ PROVIDER)")
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer providerId = getUserIdFromUserDetails(userDetails);
        VenueResponse response = venueService.createVenue(request, providerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Tạo venue thành công", response));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Cập nhật venue", description = "Cập nhật thông tin venue (chỉ PROVIDER sở hữu)")
    public ResponseEntity<ApiResponse<VenueResponse>> updateVenue(
            @PathVariable Integer id,
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer providerId = getUserIdFromUserDetails(userDetails);
        VenueResponse response = venueService.updateVenue(id, request, providerId);

        return ResponseEntity.ok(ApiResponse.success("Cập nhật venue thành công", response));
    }

    @GetMapping
    @Operation(summary = "Lấy tất cả venue", description = "Lấy danh sách tất cả venue đang có")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> response = venueService.getAllVenues();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Lấy venue theo ID", description = "Xem chi tiết một venue cụ thể")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Integer id) {
        VenueResponse response = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/my-venues")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Lấy venue của tôi", description = "Lấy danh sách venue do provider đang đăng nhập sở hữu")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getMyVenues(
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer providerId = getUserIdFromUserDetails(userDetails);
        List<VenueResponse> response = venueService.getMyVenues(providerId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('PROVIDER')")
    @Operation(summary = "Xóa venue", description = "Xóa venue theo ID (chỉ PROVIDER sở hữu)")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(
            @PathVariable Integer id,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer providerId = getUserIdFromUserDetails(userDetails);
        venueService.deleteVenue(id, providerId);
        return ResponseEntity.ok(ApiResponse.success("Xóa venue thành công", null));
    }

    private Integer getUserIdFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
