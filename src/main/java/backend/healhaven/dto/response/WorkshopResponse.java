package backend.healhaven.dto.response;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class WorkshopResponse {

    private Integer workshopId;
    private String title;
    private String category;
    private String description;
    private BigDecimal price;
    private Integer minAttendees;
    private Integer maxAttendees;
    private Integer availableSeats;
    private LocalDateTime startTime;
    private LocalDateTime endTime;
    private String status;
    private Boolean isFeatured;
    private LocalDateTime createdAt;

    // Host info
    private HostInfo host;

    // Venue info
    private VenueInfo venue;

    // Media
    private List<String> images;

    // Reviews summary
    private Double averageRating;
    private Integer reviewCount;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class HostInfo {
        private Integer userId;
        private String fullName;
        private String avatarUrl;
        private String bio;
    }

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class VenueInfo {
        private Integer venueId;
        private String name;
        private String address;
        private String district;
        private Integer capacity;
        private String amenities;
    }
}
