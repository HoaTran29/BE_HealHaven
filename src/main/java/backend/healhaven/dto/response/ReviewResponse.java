package backend.healhaven.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class ReviewResponse {

    private Integer reviewId;
    private Integer rating;
    private String comment;
    private String imageUrl;
    private LocalDateTime createdAt;

    // Reviewer info
    private ReviewerInfo reviewer;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class ReviewerInfo {
        private String fullName;
        private String avatarUrl;
    }
}
