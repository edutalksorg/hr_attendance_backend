package com.megamart.backend.sessions;

import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.time.OffsetDateTime;
import java.util.UUID;

@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SessionDto {
    private UUID id;
    private String token;
    private OffsetDateTime createdAt;
    private OffsetDateTime expiresAt;
    private boolean revoked;
}
