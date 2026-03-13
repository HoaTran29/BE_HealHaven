package backend.healhaven.dto.response;

import backend.healhaven.enums.UserRole;
import lombok.Builder;
import lombok.Data;

import java.time.LocalDateTime;

@Data
@Builder
public class AdminUserResponse {
    private Integer userId;
    private String fullName;
    private String email;
    private String phoneNumber;
    private String avatarUrl;
    private UserRole role;
    private Boolean isBanned;
    private LocalDateTime createdAt;
}
