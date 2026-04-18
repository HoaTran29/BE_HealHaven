package backend.healhaven.enums;

public enum NotificationType {
    BOOKING_APPROVED,              // Đơn thuê được phê duyệt
    BOOKING_REJECTED,              // Đơn thuê bị từ chối
    NEW_BOOKING_REQUEST,           // Có đơn thuê mới (cho provider)
    BOOKING_CANCELLED,             // Đơn thuê bị hủy
    PAYMENT_PENDING_CONFIRMATION,  // (Legacy) User xác nhận chuyển khoản thủ công
    PAYMENT_APPROVED,              // (Legacy) Admin xác nhận thanh toán
    PAYMENT_REJECTED,              // (Legacy) Admin từ chối thanh toán
    SEPAY_PAYMENT_SUCCESS,         // SePay webhook xác nhận thanh toán tự động
    SEPAY_PAYMENT_INSUFFICIENT     // SePay nhận tiền nhưng chưa đủ số tiền cần thanh toán
}
