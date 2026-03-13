package backend.healhaven.dto.response;

import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.PaymentStatus;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class BookingResponse {

    private Integer bookingId;
    private Integer id; // Alias for bookingId
    private Integer quantity;
    private BigDecimal totalPrice;
    private BigDecimal amount; // Alias for totalPrice
    private PaymentStatus paymentStatus;
    private BookingStatus bookingStatus;
    private Integer userId;
    private String userName;
    private UUID checkinCode;
    private LocalDateTime checkinAt;
    private LocalDateTime createdAt;

    // Workshop summary
    private WorkshopSummary workshop;

    // Review info (if reviewed)
    private Boolean hasReview;

    // Refund info (if requested)
    private Boolean hasRefundRequest;
    private String refundStatus;

    @Data
    @Builder
    @NoArgsConstructor
    @AllArgsConstructor
    public static class WorkshopSummary {
        private Integer workshopId;
        private String title;
        private String category;
        private String primaryImage;
        private LocalDateTime startTime;
        private LocalDateTime endTime;
        private String venueName;
        private String venueAddress;
        private String hostName;
    }
}
