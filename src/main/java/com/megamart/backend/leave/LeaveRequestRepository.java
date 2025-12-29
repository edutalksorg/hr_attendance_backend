package com.megamart.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByUserId(UUID userId);

    List<LeaveRequest> findByStatus(String status);

    @org.springframework.data.jpa.repository.Query("SELECT COUNT(DISTINCT l.userId) FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date")
    long countUsersOnLeave(java.time.LocalDate date);

    @org.springframework.data.jpa.repository.Query("SELECT l FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date")
    List<LeaveRequest> findApprovedLeavesForDate(java.time.LocalDate date);

    long countByStatus(String status);

    void deleteByCreatedAtBefore(java.time.OffsetDateTime timestamp);
}
