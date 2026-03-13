package backend.healhaven.dto.response;

import backend.healhaven.enums.WithdrawalStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
@Builder
public class WithdrawalResponse {
    private Integer withdrawalId;
    private Integer userId;
    private String fullName;
    private BigDecimal amount;
    private String bankInfo;
    private WithdrawalStatus status;
    private String note;
    private LocalDateTime requestedAt;
    private LocalDateTime processedAt;
}
