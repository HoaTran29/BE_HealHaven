package backend.healhaven.entity;

import backend.healhaven.enums.WorkshopStatus;
import jakarta.persistence.*;
import lombok.*;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.List;

@Entity
@Table(name = "workshops")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Workshop {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    @Column(name = "workshop_id")
    private Integer workshopId;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "host_id")
    private User host;

    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "venue_id")
    private Venue venue;

    @Column(nullable = false)
    private String title;

    @Column
    private String category;

    @Column(columnDefinition = "TEXT")
    private String description;

    @Column(nullable = false)
    private BigDecimal price;

    @Column(name = "min_attendees")
    private Integer minAttendees;

    @Column(name = "max_attendees", nullable = false)
    private Integer maxAttendees;

    @Column(name = "start_time", nullable = false)
    private LocalDateTime startTime;

    @Column(name = "end_time", nullable = false)
    private LocalDateTime endTime;

    @Enumerated(EnumType.STRING)
    @Column(name = "status")
    private WorkshopStatus status;

    @Column(name = "is_featured")
    private Boolean isFeatured;

    @Column(name = "created_at")
    private LocalDateTime createdAt;

    @Column(columnDefinition = "TEXT")
    private String materials;

    @Column(columnDefinition = "TEXT")
    private String itinerary;

    @Column
    private String address;

    @Column
    private String district;

    // Relationships
    @OneToMany(mappedBy = "workshop", fetch = FetchType.LAZY)
    private List<Booking> bookings;

    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        if (status == null) {
            status = WorkshopStatus.DRAFT;
        }
        if (minAttendees == null) {
            minAttendees = 1;
        }
        if (isFeatured == null) {
            isFeatured = false;
        }
    }
}
