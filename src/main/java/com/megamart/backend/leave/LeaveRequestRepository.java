package com.megamart.backend.leave;

import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface LeaveRequestRepository extends JpaRepository<LeaveRequest, UUID> {
    List<LeaveRequest> findByUserId(UUID userId);
    List<LeaveRequest> findByStatus(String status);
}
