package backend.healhaven.dto.request;

import backend.healhaven.enums.UserRole;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class UserRoleRequest {
    @NotNull(message = "Role is required")
    private UserRole role;
}
