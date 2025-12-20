package com.megamart.backend.performance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface PerformanceReviewRepository extends JpaRepository<PerformanceReview, UUID> {
    List<PerformanceReview> findByUser_Id(UUID userId);

    List<PerformanceReview> findByUser_IdIn(List<UUID> userIds);
}
