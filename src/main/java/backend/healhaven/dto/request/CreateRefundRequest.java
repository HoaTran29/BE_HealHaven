package backend.healhaven.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class CreateRefundRequest {

    @NotNull(message = "Booking ID is required")
    private Integer bookingId;

    @NotBlank(message = "Reason is required")
    private String reason;
}
