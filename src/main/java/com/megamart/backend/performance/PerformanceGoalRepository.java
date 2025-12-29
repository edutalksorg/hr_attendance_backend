package com.megamart.backend.performance;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.UUID;

public interface PerformanceGoalRepository extends JpaRepository<PerformanceGoal, UUID> {
    java.util.List<PerformanceGoal> findByUserId(UUID userId);

    java.util.List<PerformanceGoal> findByUserIdOrderByCreatedAtDesc(UUID userId);

    void deleteByCreatedAtBefore(java.time.OffsetDateTime timestamp);
}
