package backend.healhaven.dto.request;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkshopRequest {

    @NotBlank(message = "Title is required")
    private String title;

    private String category;

    private String description;

    @NotNull(message = "Price is required")
    @Positive(message = "Price must be positive")
    private BigDecimal price;

    private Integer minAttendees;

    @NotNull(message = "Max attendees is required")
    @Positive(message = "Max attendees must be positive")
    private Integer maxAttendees;

    @NotNull(message = "Start time is required")
    private LocalDateTime startTime;

    @NotNull(message = "End time is required")
    private LocalDateTime endTime;

    private Integer venueId;

    private Boolean isFeatured;
}
