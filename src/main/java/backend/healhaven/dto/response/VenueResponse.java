package backend.healhaven.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Data
@Builder
public class VenueResponse {
    private Integer venueId;
    private Integer providerId;
    private String providerName;
    private String name;
    private String address;
    private String district;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private String amenities;
    private String description;
    private String status;
    private List<String> imageUrls;
    private LocalDateTime createdAt;
}
