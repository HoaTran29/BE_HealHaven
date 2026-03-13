package backend.healhaven.dto.request;

import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserStatusRequest {
    @NotNull(message = "Status is required")
    private String status; // ACTIVE or INACTIVE
}
