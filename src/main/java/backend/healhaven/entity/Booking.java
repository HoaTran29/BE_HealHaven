package backend.healhaven.entity;

import backend.healhaven.enums.BookingStatus;
import backend.healhaven.enums.PaymentStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.UUID;

@Entity
@Table(name = "bookings")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Booking {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "booking_id")
    private Integer bookingId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "attendee_id")
    private User attendee;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "workshop_id")
    private Workshop workshop;

    @Column
    private Integer quantity;

    @Column(name = "total_price", nullable = false)
    private BigDecimal totalPrice;

    @Enumerated(EnumType.STRING)
    @Column(name = "payment_status")
    private PaymentStatus paymentStatus;

    @Enumerated(EnumType.STRING)
    @Column(name = "booking_status")
    private BookingStatus bookingStatus;

    @Column(name = "checkin_code")
    private UUID checkinCode;

    @Column(name = "checkin_at")
    private LocalDateTime checkinAt;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    // Relationships
    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private Review review;

    @OneToOne(mappedBy = "booking", fetch = FetchType.LAZY)
    private RefundRequest refundRequest;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (quantity == null) {
            quantity = 1;
        }
        if (paymentStatus == null) {
            paymentStatus = PaymentStatus.PENDING;
        }
        if (bookingStatus == null) {
            bookingStatus = BookingStatus.PENDING;
        }
        if (checkinCode == null) {
            checkinCode = UUID.randomUUID();
        }
    }
}
