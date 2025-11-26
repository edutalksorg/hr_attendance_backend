package com.megamart.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/send")
    public ResponseEntity<Notification> send(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam String type
    ) {
        Notification n = service.send(userId, title, message, type);
        return ResponseEntity.ok(n);
    }

    @GetMapping("/{userId}")
    public ResponseEntity<List<Notification>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping("/read/{id}")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        service.markRead(id);
        return ResponseEntity.ok().build();
    }
}
