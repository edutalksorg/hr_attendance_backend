package com.megamart.backend.documents;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/documents")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class DocumentController {

    private final DocumentService service;

    public static record CreateReq(@NotNull UUID userId, @NotBlank String type, @NotBlank String filePath,
            UUID generatedBy, OffsetDateTime expiresAt) {
    }

    public static record UpdateReq(@NotBlank String type, @NotBlank String filePath, OffsetDateTime expiresAt) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Document> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(201)
                .body(service.create(req.userId(), req.type(), req.filePath(), req.generatedBy(), req.expiresAt()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<Document> get(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<Document>> listForUser(@PathVariable @NonNull UUID userId) {
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Document> update(@PathVariable @NonNull UUID id, @Valid @RequestBody UpdateReq req) {
        return ResponseEntity.ok(service.update(id, req.type(), req.filePath(), req.expiresAt()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
