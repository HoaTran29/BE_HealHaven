package backend.healhaven.controller;

import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.FinancialStatsResponse;
import backend.healhaven.service.FinancialService;
import backend.healhaven.repository.UserRepository;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/financials")
@RequiredArgsConstructor
@Tag(name = "Financials", description = "Financial Management APIs")
@SecurityRequirement(name = "bearerAuth")
public class FinancialController {

    private final FinancialService financialService;
    private final UserRepository userRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasRole('HOST')")
    @Operation(summary = "Get financial stats", description = "Get total revenue, expense, and profit (HOST only)")
    public ResponseEntity<ApiResponse<FinancialStatsResponse>> getFinancialStats(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer hostId = getUserIdFromUserDetails(userDetails);
        FinancialStatsResponse response = financialService.getHostFinancialStats(hostId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    private Integer getUserIdFromUserDetails(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new backend.healhaven.exception.ResourceNotFoundException("User", "email",
                        userDetails.getUsername()))
                .getUserId();
    }
}
