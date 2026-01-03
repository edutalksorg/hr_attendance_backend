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

    @Column(length = 500)
    private String title;

    @Lob
    @Column(columnDefinition = "TEXT")
    private String message;

    private String type; // INFO | WARNING | SUCCESS | ERROR

    @Builder.Default
    private Boolean isRead = false;

    @Builder.Default
    private OffsetDateTime createdAt = OffsetDateTime.now();

    public boolean isRead() {
        return isRead != null && isRead;
    }

    public void setRead(boolean read) {
        this.isRead = read;
    }
}
