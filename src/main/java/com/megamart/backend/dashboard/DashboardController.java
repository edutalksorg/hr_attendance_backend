package com.megamart.backend.dashboard;

import com.megamart.backend.attendance.AttendanceRepository;
import com.megamart.backend.dto.DashboardStatsDTO;
import com.megamart.backend.leave.LeaveRequestRepository;
import com.megamart.backend.teams.TeamRepository;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.user.UserRole;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import java.time.LocalDate;
import java.time.OffsetDateTime;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TeamRepository teamRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<DashboardStatsDTO> getStats(
            @org.springframework.web.bind.annotation.RequestParam(required = false) java.util.UUID branchId) {
        long totalEmployees;
        long totalHR;
        long totalMarketing;
        long totalAdmins;
        long totalManagers;
        long presentToday;
        long onLeave;
        long pendingLeaves;
        long totalTeams = teamRepository.count();

        LocalDate today = LocalDate.now();
        OffsetDateTime start = today.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime end = start.plusDays(1);

        if (branchId != null) {
            totalEmployees = userRepository.countByRoleAndBranchId(UserRole.EMPLOYEE, branchId);
            totalHR = userRepository.countByRoleAndBranchId(UserRole.HR, branchId);
            totalMarketing = userRepository.countByRoleAndBranchId(UserRole.MARKETING_EXECUTIVE, branchId);
            totalAdmins = userRepository.countByRoleAndBranchId(UserRole.ADMIN, branchId);
            totalManagers = userRepository.countByRoleAndBranchId(UserRole.MANAGER, branchId);

            presentToday = attendanceRepository.countDistinctUserIdByLoginTimeBetweenAndBranchId(start, end, branchId);
            onLeave = leaveRequestRepository.countUsersOnLeaveAndBranchId(today, branchId);
            pendingLeaves = leaveRequestRepository.countByStatusAndBranchId("PENDING", branchId);
        } else {
            totalEmployees = userRepository.countByRole(UserRole.EMPLOYEE);
            totalHR = userRepository.countByRole(UserRole.HR);
            totalMarketing = userRepository.countByRole(UserRole.MARKETING_EXECUTIVE);
            totalAdmins = userRepository.countByRole(UserRole.ADMIN);
            totalManagers = userRepository.countByRole(UserRole.MANAGER);

            presentToday = attendanceRepository.countDistinctUserIdByLoginTimeBetween(start, end);
            onLeave = leaveRequestRepository.countUsersOnLeave(today);
            pendingLeaves = leaveRequestRepository.countByStatus("PENDING");
        }

        long technicalTeamCount = totalEmployees;

        return ResponseEntity.ok(new DashboardStatsDTO(
                totalEmployees + totalHR + totalMarketing + totalAdmins + totalManagers,
                totalHR,
                totalMarketing,
                presentToday,
                onLeave,
                pendingLeaves,
                totalTeams,
                technicalTeamCount,
                totalAdmins,
                totalManagers));
    }
}
