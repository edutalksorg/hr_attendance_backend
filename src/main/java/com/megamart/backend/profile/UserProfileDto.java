package com.megamart.backend.profile;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfileDto {
    private UUID id;
    private UUID userId;
    private String username;
    private String bio;
    private String photoUrl;
    private OffsetDateTime createdAt;
    private OffsetDateTime updatedAt;
}
