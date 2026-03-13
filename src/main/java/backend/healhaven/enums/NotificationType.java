package backend.healhaven.enums;

public enum NotificationType {
    BOOKING_APPROVED, // Đơn thuê được phê duyệt
    BOOKING_REJECTED, // Đơn thuê bị từ chối
    NEW_BOOKING_REQUEST, // Có đơn thuê mới (cho provider)
    BOOKING_CANCELLED, // Đơn thuê bị hủy
    PAYMENT_PENDING_CONFIRMATION, // User xác nhận chuyển khoản, thông báo Admin
    PAYMENT_APPROVED, // Admin xác nhận thanh toán thành công
    PAYMENT_REJECTED  // Admin từ chối xác nhận thanh toán
}
