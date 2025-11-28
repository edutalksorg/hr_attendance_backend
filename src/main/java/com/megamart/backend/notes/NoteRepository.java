package com.megamart.backend.notes;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface NoteRepository extends JpaRepository<Note, UUID> {
    List<Note> findByUserIdOrderByCreatedAtDesc(UUID userId);
    List<Note> findByTeamIdOrderByCreatedAtDesc(UUID teamId);
}
