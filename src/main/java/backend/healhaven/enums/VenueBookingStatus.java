package backend.healhaven.enums;

/**
 * Venue Booking Status Workflow:
 * REQUESTING -> CONFIRMED -> CANCELLED
 */
public enum VenueBookingStatus {
    REQUESTING, // Đang yêu cầu
    CONFIRMED, // Chủ chỗ đã đồng ý
    CANCELLED // Hủy đơn
}
