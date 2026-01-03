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

    java.util.Optional<Attendance> findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(UUID userId);

    List<Attendance> findByLogoutTimeIsNull();

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.userId) FROM Attendance a WHERE a.loginTime >= :start AND a.loginTime < :end")
    long countDistinctUserIdByLoginTimeBetween(java.time.OffsetDateTime start, java.time.OffsetDateTime end);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT a.userId) FROM Attendance a WHERE a.loginTime >= :start AND a.loginTime < :end AND a.userId IN (SELECT u.id FROM User u WHERE u.branch.id = :branchId)")
    long countDistinctUserIdByLoginTimeBetweenAndBranchId(java.time.OffsetDateTime start, java.time.OffsetDateTime end,
            @org.springframework.data.repository.query.Param("branchId") UUID branchId);
}
