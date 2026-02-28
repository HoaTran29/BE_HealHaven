package backend.healhaven.dto.response;

import backend.healhaven.enums.NotificationType;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class NotificationResponse {
    private Integer notificationId;
    private String title;
    private String message;
    private NotificationType type;
    private Integer referenceId;
    private Boolean isRead;
    private LocalDateTime createdAt;
}
