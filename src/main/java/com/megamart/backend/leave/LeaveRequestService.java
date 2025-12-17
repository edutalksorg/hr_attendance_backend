package com.megamart.backend.leave;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.LocalDate;
import org.springframework.lang.NonNull;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LeaveRequestService {
    private final LeaveRequestRepository repository;

    public LeaveRequest requestLeave(UUID userId, String leaveType, LocalDate startDate, LocalDate endDate,
            String reason) {
        LeaveRequest lr = LeaveRequest.builder()
                .userId(userId)
                .leaveType(leaveType)
                .startDate(startDate)
                .endDate(endDate)
                .reason(reason)
                .status("PENDING")
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return repository.save(lr);
    }

    public LeaveRequest getLeaveRequest(@NonNull UUID id) {
        return repository.findById(id).orElseThrow(() -> new RuntimeException("Leave request not found"));
    }

    public List<LeaveRequest> getUserLeaveRequests(UUID userId) {
        return repository.findByUserId(userId);
    }

    public List<LeaveRequest> getPendingLeaveRequests() {
        return repository.findByStatus("PENDING");
    }

    public List<LeaveRequest> getApprovedLeaves(LocalDate date) {
        return repository.findApprovedLeavesForDate(date);
    }

    public LeaveRequest approveLeaveRequest(UUID leaveRequestId, UUID approverUserId) {
        LeaveRequest lr = getLeaveRequest(leaveRequestId);
        lr.setStatus("APPROVED");
        lr.setApprovedBy(approverUserId);
        lr.setApprovedAt(OffsetDateTime.now());
        lr.setUpdatedAt(OffsetDateTime.now());
        return repository.save(lr);
    }

    public LeaveRequest rejectLeaveRequest(UUID leaveRequestId, UUID approverUserId) {
        LeaveRequest lr = getLeaveRequest(leaveRequestId);
        lr.setStatus("REJECTED");
        lr.setApprovedBy(approverUserId);
        lr.setApprovedAt(OffsetDateTime.now());
        lr.setUpdatedAt(OffsetDateTime.now());
        return repository.save(lr);
    }
}
