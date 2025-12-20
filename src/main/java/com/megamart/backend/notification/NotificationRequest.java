package com.megamart.backend.notification;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import lombok.Data;
import java.util.UUID;

@Data
public class NotificationRequest {
    private String userId; // Optional for broadcast, String to allow manual parsing validation if needed

    private java.util.List<String> userIds; // For multiple users
    private String teamId; // For team-based notifications

    @NotBlank(message = "Title is required")
    @Size(max = 500, message = "Title cannot exceed 500 characters")
    private String title;

    @NotBlank(message = "Message is required")
    @Size(max = 10000, message = "Message cannot exceed 10000 characters")
    private String message;

    private String type = "INFO";
}
