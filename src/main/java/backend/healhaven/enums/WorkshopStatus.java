package backend.healhaven.enums;

/**
 * Workshop Status Workflow:
 * DRAFT -> PENDING_APPROVAL -> PUBLISHED -> HAPPENING -> CLOSED
 */
public enum WorkshopStatus {
    DRAFT, // Nháp
    PENDING_APPROVAL, // Chờ duyệt
    PUBLISHED, // Đang hiển thị
    HAPPENING, // Đang diễn ra
    CLOSED, // Đã đóng
    REJECTED // Đã bị từ chối duyệt
}
