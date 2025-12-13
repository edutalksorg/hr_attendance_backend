package com.megamart.backend.attendance;

import org.springframework.data.jpa.repository.JpaRepository;
import java.util.List;
import java.util.UUID;

public interface AttendanceRepository extends JpaRepository<Attendance, UUID> {
    List<Attendance> findByUserIdOrderByCreatedAtDesc(UUID userId);

    List<Attendance> findByUserIdInOrderByCreatedAtDesc(java.util.List<UUID> userIds);

    void deleteByCreatedAtBefore(java.time.OffsetDateTime timestamp);

    List<Attendance> findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(UUID userId, java.time.OffsetDateTime start,
            java.time.OffsetDateTime end);
}
