package com.megamart.backend.documents;

import jakarta.persistence.*;
import lombok.*;

import java.time.OffsetDateTime;
import java.util.UUID;

@Entity
@Table(name = "documents")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class Document {
    @Id
    @GeneratedValue
    private UUID id;

    @Column(name = "user_id", nullable = false)
    private UUID userId;

    @Column(name = "type", nullable = false)
    private String type;

    @Column(name = "file_path", nullable = false)
    private String filePath;

    @Column(name = "generated_by")
    private UUID generatedBy;

    @Column(name = "generated_at")
    private OffsetDateTime generatedAt;

    @Column(name = "expires_at")
    private OffsetDateTime expiresAt;

    @Builder.Default
    @Column(name = "created_at")
    private OffsetDateTime createdAt = OffsetDateTime.now();
}
