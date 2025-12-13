package com.megamart.backend.documents;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentService {
    private final DocumentRepository repository;

    public Document create(@NonNull UUID userId, String type, String filePath, @NonNull UUID generatedBy,
            OffsetDateTime expiresAt) {
        Document d = Document.builder()
                .userId(userId)
                .type(type)
                .filePath(filePath)
                .generatedBy(generatedBy)
                .generatedAt(OffsetDateTime.now())
                .expiresAt(expiresAt)
                .createdAt(OffsetDateTime.now())
                .build();
        return repository.save(d);
    }

    public Document get(@NonNull UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Document not found"));
    }

    public List<Document> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public Document update(UUID id, String type, String filePath, OffsetDateTime expiresAt) {
        Document d = get(id);
        d.setType(type);
        d.setFilePath(filePath);
        d.setExpiresAt(expiresAt);
        return repository.save(d);
    }

    public void delete(@NonNull UUID id) {
        repository.deleteById(id);
    }
}
