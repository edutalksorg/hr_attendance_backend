package com.megamart.backend.notes;

import jakarta.annotation.PostConstruct;
import org.springframework.jdbc.core.JdbcTemplate;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NoteService {
    private final NoteRepository repository;
    private final JdbcTemplate jdbcTemplate;

    @PostConstruct
    public void init() {
        try {
            jdbcTemplate.execute("ALTER TABLE notes ADD COLUMN IF NOT EXISTS is_pinned BOOLEAN DEFAULT FALSE");
            jdbcTemplate.execute("UPDATE notes SET is_pinned = FALSE WHERE is_pinned IS NULL");
        } catch (Exception e) {
            // Ignore error if column already exists or table doesn't exist yet
            System.err.println("NoteService: Failed to ensure is_pinned column: " + e.getMessage());
        }
    }

    public Note create(UUID userId, UUID teamId, String title, String body, boolean isPinned) {
        Note n = Note.builder().userId(userId).teamId(teamId).title(title).body(body).pinned(isPinned)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now()).build();
        return repository.save(n);
    }

    public Note get(@NonNull UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Note not found"));
    }

    public List<Note> listForUser(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    public List<Note> listForTeam(UUID teamId) {
        return repository.findByTeamIdOrderByCreatedAtDesc(teamId);
    }

    public Note update(UUID id, String title, String body, boolean isPinned) {
        Note n = get(id);
        n.setTitle(title);
        n.setBody(body);
        n.setPinned(isPinned);
        n.setUpdatedAt(OffsetDateTime.now());
        return repository.save(n);
    }

    public void delete(@NonNull UUID id) {
        repository.deleteById(id);
    }
}
