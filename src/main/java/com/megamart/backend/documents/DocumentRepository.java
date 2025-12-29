package com.megamart.backend.documents;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface DocumentRepository extends JpaRepository<Document, UUID> {
    java.util.List<Document> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByCreatedAtBefore(java.time.OffsetDateTime timestamp);

    java.util.List<Document> findByCreatedAtBefore(java.time.OffsetDateTime timestamp);
}
