package com.megamart.backend.notes;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/notes")
@RequiredArgsConstructor
public class NoteController {
    private final NoteService service;

    public static record CreateReq(@NotNull UUID userId, UUID teamId, @NotBlank String title, String body,
            boolean isPinned) {
    }

    public static record UpdateReq(@NotBlank String title, String body, boolean isPinned) {
    }

    @PostMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Note> create(@Valid @RequestBody CreateReq req) {
        return ResponseEntity.status(201)
                .body(service.create(req.userId(), req.teamId(), req.title(), req.body(), req.isPinned()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Note> get(@PathVariable @NonNull UUID id) {
        return ResponseEntity.ok(service.get(id));
    }

    @GetMapping("/user/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Note>> listForUser(@PathVariable @NonNull UUID userId) {
        return ResponseEntity.ok(service.listForUser(userId));
    }

    @GetMapping("/team/{teamId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<Note>> listForTeam(@PathVariable @NonNull UUID teamId) {
        return ResponseEntity.ok(service.listForTeam(teamId));
    }

    @PutMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Note> update(@PathVariable @NonNull UUID id, @Valid @RequestBody UpdateReq req) {
        return ResponseEntity.ok(service.update(id, req.title(), req.body(), req.isPinned()));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        service.delete(id);
        return ResponseEntity.ok().build();
    }
}
