package backend.healhaven.dto.response;

import lombok.Builder;
import lombok.Data;
import java.math.BigDecimal;

@Data
@Builder
public class FinancialStatsResponse {
    private BigDecimal totalRevenue;
    private BigDecimal totalExpense;
    private BigDecimal netProfit;
    private Integer bookingCount; // Optional: extra insight
}
