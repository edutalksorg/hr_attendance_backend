package com.megamart.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<Notification> send(
            @RequestParam UUID userId,
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "INFO") String type
    ) {
        Notification n = service.send(userId, title, message, type);
        return ResponseEntity.status(201).body(n);
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<Notification>> list(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping("/read/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<Void> markRead(@PathVariable UUID id) {
        service.markRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-notifications")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<Notification>> getMyNotifications() {
        // Extract user from token - for now admin only test version
        // In production, use SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(service.list(UUID.randomUUID()));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> broadcastNotification(
            @RequestParam String title,
            @RequestParam String message,
            @RequestParam(defaultValue = "INFO") String type
    ) {
        // This is a stub - in production, notify all users or by role
        return ResponseEntity.ok().build();
    }
}
