package backend.healhaven.enums;

public enum NotificationType {
    BOOKING_APPROVED, // Đơn thuê được phê duyệt
    BOOKING_REJECTED, // Đơn thuê bị từ chối
    NEW_BOOKING_REQUEST, // Có đơn thuê mới (cho provider)
    BOOKING_CANCELLED // Đơn thuê bị hủy
}
