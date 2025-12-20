package com.megamart.backend.helpdesk;

import com.megamart.backend.user.User;
import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "support_tickets")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SupportTicket {
    @Id
    @GeneratedValue
    private UUID id;

    @ManyToOne
    @JoinColumn(name = "requester_id", nullable = false)
    private User requester;

    // Type: IT_SUPPORT, HR_QUERY, PAYROLL_ISSUE, GENERAL
    private String category;

    // Low, Medium, High
    private String priority;

    private String subject;

    @Column(columnDefinition = "TEXT")
    private String description;

    // OPEN, IN_PROGRESS, RESOLVED, REJECTED
    @Builder.Default
    private String status = "OPEN";

    @ManyToOne
    @JoinColumn(name = "assigned_to")
    private User assignedTo; // Admin or HR

    @Column(name = "created_at")
    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    @Column(name = "updated_at")
    private OffsetDateTime updatedAt;

    @Column(columnDefinition = "TEXT")
    private String attachmentUrl;
}
