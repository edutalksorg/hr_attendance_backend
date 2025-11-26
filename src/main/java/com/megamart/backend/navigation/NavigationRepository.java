package com.megamart.backend.navigation;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface NavigationRepository extends JpaRepository<NavigationLog, UUID> {
    List<NavigationLog> findByUserIdOrderByCreatedAtDesc(UUID userId);
}
