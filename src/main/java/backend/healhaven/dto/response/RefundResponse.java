package backend.healhaven.dto.response;

import backend.healhaven.enums.RefundStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class RefundResponse {

    private Integer refundId;
    private Integer bookingId;
    private String reason;
    private BigDecimal refundAmount;
    private RefundStatus status;
    private LocalDateTime requestedAt;

    // Workshop info
    private String workshopTitle;
    private BigDecimal originalAmount;
}
