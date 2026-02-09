package backend.healhaven.dto.response;

import backend.healhaven.enums.VenueBookingStatus;
import lombok.Builder;
import lombok.Data;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalTime;

@Data
@Builder
public class VenueBookingResponse {
    private Integer bookingId;
    private Integer venueId;
    private String venueName;
    private String venueAddress;
    private Integer hostId;
    private String hostName;
    private LocalDate bookingDate;
    private LocalTime startTime;
    private LocalTime endTime;
    private BigDecimal totalCost;
    private VenueBookingStatus status;
}
