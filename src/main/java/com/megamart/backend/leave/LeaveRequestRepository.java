package com.megamart.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
        List<LeaveRequest> findByUserId(UUID userId);

        List<LeaveRequest> findByStatus(String status);

        @Query("SELECT l FROM LeaveRequest l WHERE l.status = :status AND l.userId IN (SELECT u.id FROM User u WHERE u.branch.id = :branchId)")
        List<LeaveRequest> findByStatusAndBranchId(@Param("status") String status, @Param("branchId") UUID branchId);

        @Query("SELECT COUNT(DISTINCT l.userId) FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date")
        long countUsersOnLeave(@Param("date") LocalDate date);

        @Query("SELECT COUNT(DISTINCT l.userId) FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date AND l.userId IN (SELECT u.id FROM User u WHERE u.branch.id = :branchId)")
        long countUsersOnLeaveAndBranchId(@Param("date") LocalDate date, @Param("branchId") UUID branchId);

        @Query("SELECT l FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date")
        List<LeaveRequest> findApprovedLeavesForDate(@Param("date") LocalDate date);

        @Query("SELECT l FROM LeaveRequest l WHERE l.status = 'APPROVED' AND l.startDate <= :date AND l.endDate >= :date AND l.userId IN (SELECT u.id FROM User u WHERE u.branch.id = :branchId)")
        List<LeaveRequest> findApprovedLeavesForDateAndBranchId(@Param("date") LocalDate date,
                        @Param("branchId") UUID branchId);

        long countByStatus(String status);

        @Query("SELECT COUNT(l) FROM LeaveRequest l WHERE l.status = :status AND l.userId IN (SELECT u.id FROM User u WHERE u.branch.id = :branchId)")
        long countByStatusAndBranchId(@Param("status") String status, @Param("branchId") UUID branchId);

        void deleteByCreatedAtBefore(OffsetDateTime timestamp);
}
