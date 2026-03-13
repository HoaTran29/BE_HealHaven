package backend.healhaven.dto.request;

import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RejectReasonRequest {
    @NotBlank(message = "Reason is required when rejecting")
    private String reason;
}
