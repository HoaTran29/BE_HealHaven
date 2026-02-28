package backend.healhaven.service;

import backend.healhaven.dto.response.NotificationResponse;
import backend.healhaven.entity.Notification;
import backend.healhaven.entity.User;
import backend.healhaven.enums.NotificationType;
import backend.healhaven.exception.ResourceNotFoundException;
import backend.healhaven.repository.NotificationRepository;
import backend.healhaven.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class NotificationService {

    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    /**
     * Gửi thông báo cho user
     */
    @Transactional
    public void sendNotification(Integer userId, String title, String message,
            NotificationType type, Integer referenceId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new ResourceNotFoundException("User", "id", userId));

        Notification notification = Notification.builder()
                .user(user)
                .title(title)
                .message(message)
                .type(type)
                .referenceId(referenceId)
                .isRead(false)
                .build();

        notificationRepository.save(notification);
    }

    /**
     * Lấy tất cả thông báo của user
     */
    public List<NotificationResponse> getMyNotifications(Integer userId) {
        return notificationRepository.findByUserUserIdOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Lấy thông báo chưa đọc
     */
    public List<NotificationResponse> getUnreadNotifications(Integer userId) {
        return notificationRepository.findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId)
                .stream()
                .map(this::mapToResponse)
                .collect(Collectors.toList());
    }

    /**
     * Đếm số thông báo chưa đọc
     */
    public int getUnreadCount(Integer userId) {
        return notificationRepository.countByUserUserIdAndIsReadFalse(userId);
    }

    /**
     * Đánh dấu đã đọc
     */
    @Transactional
    public void markAsRead(Integer notificationId, Integer userId) {
        Notification notification = notificationRepository.findById(notificationId)
                .orElseThrow(() -> new ResourceNotFoundException("Notification", "id", notificationId));

        if (!notification.getUser().getUserId().equals(userId)) {
            throw new backend.healhaven.exception.BadRequestException("Bạn không có quyền truy cập thông báo này");
        }

        notification.setIsRead(true);
        notificationRepository.save(notification);
    }

    /**
     * Đánh dấu tất cả đã đọc
     */
    @Transactional
    public void markAllAsRead(Integer userId) {
        List<Notification> unread = notificationRepository.findByUserUserIdAndIsReadFalseOrderByCreatedAtDesc(userId);
        unread.forEach(n -> n.setIsRead(true));
        notificationRepository.saveAll(unread);
    }

    private NotificationResponse mapToResponse(Notification notification) {
        return NotificationResponse.builder()
                .notificationId(notification.getNotificationId())
                .title(notification.getTitle())
                .message(notification.getMessage())
                .type(notification.getType())
                .referenceId(notification.getReferenceId())
                .isRead(notification.getIsRead())
                .createdAt(notification.getCreatedAt())
                .build();
    }
}
