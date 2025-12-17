package com.megamart.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/notifications")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class NotificationController {

    private final NotificationService service;

    @PostMapping("/send")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<?> send(@RequestBody @jakarta.validation.Valid NotificationRequest request) {
        try {
            UUID parsedUserId = UUID.fromString(request.getUserId());
            Notification n = service.send(parsedUserId, request.getTitle(), request.getMessage(), request.getType());
            return ResponseEntity.status(201).body(n);
        } catch (IllegalArgumentException e) {
            System.err.println("Invalid UUID format for userId: " + request.getUserId());
            return ResponseEntity.badRequest().body(java.util.Map.of(
                    "error", "Invalid user ID format",
                    "message", e.getMessage()));
        } catch (Exception e) {
            System.err.println("Error sending notification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to send notification",
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<Notification>> list(@PathVariable @NonNull UUID userId) {
        return ResponseEntity.ok(service.list(userId));
    }

    @PostMapping("/read/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<Void> markRead(@PathVariable @NonNull UUID id) {
        service.markRead(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/my-notifications")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<Notification>> getMyNotifications() {
        // Extract user from token - for now admin only test version
        // In production, use
        // SecurityContextHolder.getContext().getAuthentication().getName();
        return ResponseEntity.ok(service.list(UUID.randomUUID()));
    }

    @PostMapping("/broadcast")
    @PreAuthorize("hasAnyRole('ADMIN', 'HR')")
    public ResponseEntity<?> broadcastNotification(@RequestBody @jakarta.validation.Valid NotificationRequest request) {
        try {
            service.broadcast(request.getTitle(), request.getMessage(), request.getType());
            return ResponseEntity.ok().build();
        } catch (Exception e) {
            System.err.println("Error broadcasting notification: " + e.getMessage());
            e.printStackTrace();
            return ResponseEntity.status(500).body(java.util.Map.of(
                    "error", "Failed to broadcast notification",
                    "message", e.getMessage() != null ? e.getMessage() : "Unknown error"));
        }
    }
}
