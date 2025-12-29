package com.megamart.backend.navigation;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class NavigationService {
    private final NavigationRepository repo;

    public NavigationLog log(UUID userId, String path, String metadata, String ip) {
        NavigationLog l = NavigationLog.builder()
                .userId(userId)
                .path(path)
                .metadata(metadata)
                .ipAddress(ip)
                .createdAt(OffsetDateTime.now())
                .build();
        return repo.save(l);
    }

    public List<NavigationLog> history(UUID userId) {
        return repo.findByUserIdOrderByCreatedAtDesc(userId);
    }
}
