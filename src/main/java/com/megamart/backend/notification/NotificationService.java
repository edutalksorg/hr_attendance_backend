package com.megamart.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;

    // SEND NOTIFICATION
    public Notification send(UUID userId, String title, String message, String type) {
        Notification n = Notification.builder()
                .userId(userId)
                .title(title)
                .message(message)
                .type(type)
                .isRead(false)
                .createdAt(OffsetDateTime.now())
                .build();

        return repository.save(n);
    }

    // LIST FOR USER
    public List<Notification> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // MARK READ
    public void markRead(UUID id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        n.setRead(true);
        repository.save(n);
    }
}
