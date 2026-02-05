package backend.healhaven.dto.request;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import lombok.Data;

@Data
public class BookingRequest {

    @NotNull(message = "Workshop ID is required")
    private Integer workshopId;

    @Min(value = 1, message = "Quantity must be at least 1")
    private Integer quantity = 1;
}
