package backend.healhaven.controller;

import backend.healhaven.dto.response.ApiResponse;
import backend.healhaven.dto.response.NotificationResponse;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.UserRepository;
import backend.healhaven.service.NotificationService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.security.core.userdetails.UserDetails;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@RestController
@RequestMapping("/notifications")
@RequiredArgsConstructor
@Tag(name = "Notifications", description = "Notification APIs")
public class NotificationController {

    private final NotificationService notificationService;
    private final UserRepository userRepository;

    @GetMapping
    @Operation(summary = "Lấy thông báo", description = "Lấy tất cả thông báo của tôi")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getMyNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserId(userDetails);
        List<NotificationResponse> response = notificationService.getMyNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread")
    @Operation(summary = "Thông báo chưa đọc", description = "Lấy danh sách thông báo chưa đọc")
    public ResponseEntity<ApiResponse<List<NotificationResponse>>> getUnreadNotifications(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserId(userDetails);
        List<NotificationResponse> response = notificationService.getUnreadNotifications(userId);
        return ResponseEntity.ok(ApiResponse.success(response));
    }

    @GetMapping("/unread-count")
    @Operation(summary = "Đếm chưa đọc", description = "Đếm số thông báo chưa đọc")
    public ResponseEntity<ApiResponse<Map<String, Integer>>> getUnreadCount(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserId(userDetails);
        int count = notificationService.getUnreadCount(userId);
        return ResponseEntity.ok(ApiResponse.success(Map.of("unreadCount", count)));
    }

    @PutMapping("/{notificationId}/read")
    @Operation(summary = "Đánh dấu đã đọc", description = "Đánh dấu 1 thông báo đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAsRead(
            @PathVariable Integer notificationId,
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserId(userDetails);
        notificationService.markAsRead(notificationId, userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu đã đọc", null));
    }

    @PutMapping("/read-all")
    @Operation(summary = "Đọc tất cả", description = "Đánh dấu tất cả thông báo đã đọc")
    public ResponseEntity<ApiResponse<Void>> markAllAsRead(
            @AuthenticationPrincipal UserDetails userDetails) {
        Integer userId = getUserId(userDetails);
        notificationService.markAllAsRead(userId);
        return ResponseEntity.ok(ApiResponse.success("Đã đánh dấu tất cả đã đọc", null));
    }

    private Integer getUserId(UserDetails userDetails) {
        return userRepository.findByEmail(userDetails.getUsername())
                .orElseThrow(() -> new ResourceNotFoundException("User", "email", userDetails.getUsername()))
                .getUserId();
    }
}
