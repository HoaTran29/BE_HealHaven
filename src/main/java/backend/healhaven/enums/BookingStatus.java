package backend.healhaven.enums;

/**
 * Booking Status Workflow:
 * PENDING -> PAID -> ATTENDED -> COMPLETED -> REVIEWED
 */
public enum BookingStatus {
    PENDING, // Chờ thanh toán
    PAID, // Đã thanh toán
    ATTENDED, // Đã điểm danh
    COMPLETED, // Workshop đã kết thúc
    REVIEWED // Đã đánh giá
}
