package com.megamart.backend.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PerformanceGoalRepository extends JpaRepository<PerformanceGoal, UUID> {
    List<PerformanceGoal> findByUserId(UUID userId);
}
