package backend.healhaven.enums;

public enum PaymentStatus {
    PENDING,               // Chờ thanh toán
    PENDING_CONFIRMATION,  // User đã xác nhận chuyển khoản, chờ admin duyệt
    PAID,                  // Đã thanh toán (admin đã xác nhận)
    REFUNDED               // Đã hoàn tiền
}
