package backend.healhaven.dto.response;

import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;

@Data
@Builder
public class AdminStatsResponse {
    private long totalUsers;
    private long totalWorkshops;
    private long totalVenues;
    private BigDecimal netRevenue;
}
