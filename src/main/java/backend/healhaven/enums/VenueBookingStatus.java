package backend.healhaven.enums;

/**
 * Venue Booking Status Workflow:
 * REQUESTING -> CONFIRMED (approved by provider)
 * REQUESTING -> REJECTED (rejected by provider)
 * CONFIRMED -> CANCELLED (cancelled by host or provider)
 */
public enum VenueBookingStatus {
    REQUESTING, // Đang yêu cầu - chờ provider duyệt
    CONFIRMED, // Provider đã phê duyệt
    REJECTED, // Provider đã từ chối
    CANCELLED // Đã hủy
}
