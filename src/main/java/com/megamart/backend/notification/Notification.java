package com.megamart.backend.notification;

import jakarta.persistence.*;
import lombok.*;
import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "notifications")
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class Notification {

    @Id
    @GeneratedValue
    private UUID id;

    private UUID userId;

    private String title;

    private String message;

    private String type;  // INFO | WARNING | SUCCESS | ERROR

    @Builder.Default
    private boolean isRead = false;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
