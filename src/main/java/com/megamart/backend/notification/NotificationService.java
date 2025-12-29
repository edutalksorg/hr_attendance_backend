package com.megamart.backend.notification;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NotificationService {

    private final NotificationRepository repository;
    private final com.megamart.backend.email.EmailService emailService;
    private final com.megamart.backend.user.UserRepository userRepository;

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

        // Save notification first to ensure it's created even if email fails
        Notification saved = repository.save(n);

        // Send Email (don't let email failures crash the notification creation)
        try {
            userRepository.findById(userId).ifPresent(user -> {
                if (user.getEmail() != null && !user.getEmail().isEmpty()) {
                    // Async call is handled inside EmailService
                    emailService.sendEmail(user.getEmail(), title, message);
                }
            });
        } catch (Exception e) {
            // Log but don't propagate - notification was already saved
            System.err.println("Failed to send email for notification: " + e.getMessage());
            e.printStackTrace();
        }

        return saved;
    }

    // LIST FOR USER
    public List<Notification> list(UUID userId) {
        return repository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    // MARK READ
    public void markRead(@NonNull UUID id) {
        Notification n = repository.findById(id)
                .orElseThrow(() -> new RuntimeException("Notification not found"));

        n.setRead(true);
        repository.save(n);
    }

    // BROADCAST
    public void broadcast(String title, String message, String type) {
        List<com.megamart.backend.user.User> users = userRepository.findAll();
        for (com.megamart.backend.user.User user : users) {
            // Avoid sending to deleted/blocked users if needed, but for now send to all
            if (user.getStatus() == com.megamart.backend.user.UserStatus.ACTIVE) {
                send(user.getId(), title, message, type);
            }
        }
    }

    // BATCH SEND
    public void sendBatch(List<UUID> userIds, String title, String message, String type) {
        if (userIds == null)
            return;
        for (UUID uid : userIds) {
            send(uid, title, message, type);
        }
    }

    // TEAM SEND
    private final com.megamart.backend.teams.TeamMemberRepository teamMemberRepository;

    public void sendToTeam(UUID teamId, String title, String message, String type) {
        List<com.megamart.backend.teams.TeamMember> members = teamMemberRepository.findByTeamId(teamId);
        for (com.megamart.backend.teams.TeamMember m : members) {
            send(m.getUserId(), title, message, type);
        }
    }
}
