package com.megamart.backend.leave;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.RequiredArgsConstructor;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;

import java.time.LocalDate;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/leave")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class LeaveController {
    private final LeaveRequestService service;
    private final UserRepository userRepository;

    public static record RequestLeaveReq(
            @NotBlank String leaveType,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate startDate,
            @NotNull @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate endDate,
            String reason) {
    }

    @PostMapping("/request")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MARKETING_EXECUTIVE','HR')")
    public ResponseEntity<LeaveRequestDto> requestLeave(@RequestBody RequestLeaveReq req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        LeaveRequest lr = service.requestLeave(user.getId(), req.leaveType(), req.startDate(), req.endDate(),
                req.reason());
        return ResponseEntity.status(201).body(toDto(lr));
    }

    @GetMapping("/my-requests")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MARKETING_EXECUTIVE','HR')")
    public ResponseEntity<List<LeaveRequestDto>> getMyLeaveRequests() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<LeaveRequest> requests = service.getUserLeaveRequests(user.getId());
        return ResponseEntity.ok(requests.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/pending")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<List<LeaveRequestDto>> getPendingLeaveRequests() {
        List<LeaveRequest> requests = service.getPendingLeaveRequests();
        return ResponseEntity.ok(requests.stream().map(this::toDto).collect(Collectors.toList()));
    }

    @GetMapping("/{id}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','MARKETING_EXECUTIVE','HR','ADMIN')")
    public ResponseEntity<LeaveRequestDto> getLeaveRequest(@PathVariable @NonNull UUID id) {
        LeaveRequest lr = service.getLeaveRequest(id);
        return ResponseEntity.ok(toDto(lr));
    }

    @PostMapping("/{id}/approve")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<LeaveRequestDto> approveLeaveRequest(@PathVariable @NonNull UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        LeaveRequest lr = service.approveLeaveRequest(id, approver.getId());
        return ResponseEntity.ok(toDto(lr));
    }

    @PostMapping("/{id}/reject")
    @PreAuthorize("hasAnyRole('HR','ADMIN')")
    public ResponseEntity<LeaveRequestDto> rejectLeaveRequest(@PathVariable @NonNull UUID id) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        LeaveRequest lr = service.rejectLeaveRequest(id, approver.getId());
        return ResponseEntity.ok(toDto(lr));
    }

    private LeaveRequestDto toDto(LeaveRequest lr) {
        return LeaveRequestDto.builder()
                .id(lr.getId())
                .userId(lr.getUserId())
                .leaveType(lr.getLeaveType())
                .startDate(lr.getStartDate())
                .endDate(lr.getEndDate())
                .reason(lr.getReason())
                .status(lr.getStatus())
                .approvedBy(lr.getApprovedBy())
                .approvedAt(lr.getApprovedAt())
                .createdAt(lr.getCreatedAt())
                .updatedAt(lr.getUpdatedAt())
                .build();
    }
}
