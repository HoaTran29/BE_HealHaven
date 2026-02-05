package backend.healhaven.dto.request;

import lombok.Data;
import java.math.BigDecimal;
import java.time.LocalDateTime;

@Data
public class WorkshopSearchRequest {

    private String keyword;
    private String category;
    private String district;
    private BigDecimal minPrice;
    private BigDecimal maxPrice;
    private LocalDateTime startDate;
    private Integer hostId;

    // Pagination
    private Integer page = 0;
    private Integer size = 10;
    private String sortBy = "startTime";
    private String sortDir = "asc";
}
