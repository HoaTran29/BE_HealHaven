package backend.healhaven.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class VenueResponse {
    private Integer venueId;
    private Integer providerId;
    private String name;
    private String address;
    private String district;
    private Integer capacity;
    private BigDecimal pricePerHour;
    private String amenities;
    private String description;
    private String status;
}
