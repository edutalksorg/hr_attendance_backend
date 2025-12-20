package com.megamart.backend.performance;

import com.megamart.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "performance_goals")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class PerformanceGoal {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "user_id", nullable = false)
    private User user;

    private String title;

    @Column(columnDefinition = "TEXT")
    private String description;

    // Type: MONTHLY, QUARTERLY, YEARLY
    private String type;

    private LocalDate startDate;
    private LocalDate endDate;

    // Status: PENDING, IN_PROGRESS, COMPLETED, MISSED
    private String status;

    private Integer progressPercentage; // 0-100

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "feedback", columnDefinition = "TEXT")
    private String adminFeedback;
}
