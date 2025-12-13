package com.megamart.backend.notes;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class NoteService {
    private final NoteRepository repository;

    public Note create(UUID userId, UUID teamId, String title, String body) {
        Note n = Note.builder().userId(userId).teamId(teamId).title(title).body(body).createdAt(OffsetDateTime.now())
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

    public Note update(UUID id, String title, String body) {
        Note n = get(id);
        n.setTitle(title);
        n.setBody(body);
        n.setUpdatedAt(OffsetDateTime.now());
        return repository.save(n);
    }

    public void delete(@NonNull UUID id) {
        repository.deleteById(id);
    }
}
