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
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/venues")
@RequiredArgsConstructor
@Tag(name = "Venues", description = "Venue Management APIs")
public class VenueController {

    private final VenueService venueService;
    private final UserRepository userRepository;

    @PostMapping
    @Operation(summary = "Create a venue", description = "Create a new venue (Requires PROVIDER or ADMIN role)")
    // @PreAuthorize("hasAnyRole('PROVIDER', 'ADMIN')") // Uncomment if you want to
    // restrict role
    public ResponseEntity<ApiResponse<VenueResponse>> createVenue(
            @Valid @RequestBody VenueRequest request,
            @AuthenticationPrincipal UserDetails userDetails) {

        Integer providerId = getUserIdFromUserDetails(userDetails);
        VenueResponse response = venueService.createVenue(request, providerId);

        return ResponseEntity.status(HttpStatus.CREATED)
                .body(ApiResponse.success("Venue created successfully", response));
    }

    @GetMapping
    @Operation(summary = "Get all venues", description = "Get a list of all available venues")
    public ResponseEntity<ApiResponse<List<VenueResponse>>> getAllVenues() {
        List<VenueResponse> response = venueService.getAllVenues();
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/{id}")
    @Operation(summary = "Get venue by ID", description = "Get details of a specific venue")
    public ResponseEntity<ApiResponse<VenueResponse>> getVenueById(@PathVariable Integer id) {
        VenueResponse response = venueService.getVenueById(id);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @DeleteMapping("/{id}")
    @Operation(summary = "Delete venue", description = "Delete a venue by ID")
    public ResponseEntity<ApiResponse<Void>> deleteVenue(@PathVariable Integer id) {
        venueService.deleteVenue(id);
        return ResponseEntity.ok(ApiResponse.success("Venue deleted successfully", null));
    }

    private Integer getUserIdFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
