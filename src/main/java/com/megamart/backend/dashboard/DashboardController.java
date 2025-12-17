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
import java.time.ZoneOffset;

@RestController
@RequestMapping("/api/v1/dashboard")
@RequiredArgsConstructor
public class DashboardController {

    private final UserRepository userRepository;
    private final AttendanceRepository attendanceRepository;
    private final LeaveRequestRepository leaveRequestRepository;
    private final TeamRepository teamRepository;

    @GetMapping("/stats")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<DashboardStatsDTO> getStats() {
        long totalEmployees = userRepository.countByRole(UserRole.EMPLOYEE);
        long totalHR = userRepository.countByRole(UserRole.HR);
        long totalMarketing = userRepository.countByRole(UserRole.MARKETING_EXECUTIVE);
        long totalAdmins = userRepository.countByRole(UserRole.ADMIN);

        LocalDate today = LocalDate.now();
        OffsetDateTime startOfDay = today.atStartOfDay().atOffset(ZoneOffset.UTC); // Assuming UTC for simplicity or
                                                                                   // adjust to system zone
        // For better accuracy, use system default zone or configured zone
        // OffsetDateTime.now() uses system clock, so let's use:
        OffsetDateTime start = LocalDate.now().atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime end = start.plusDays(1);

        long presentToday = attendanceRepository.countDistinctUserIdByLoginTimeBetween(start, end);
        long onLeave = leaveRequestRepository.countUsersOnLeave(today);
        long pendingLeaves = leaveRequestRepository.countByStatus("PENDING");
        long totalTeams = teamRepository.count();
        long technicalTeamCount = userRepository.countByRole(UserRole.EMPLOYEE);

        // Refine marketing count if there is 'MARKETING' role too
        // long marketingManager = userRepository.countByRole(Role.MARKETING);
        // totalMarketing += marketingManager;

        return ResponseEntity.ok(new DashboardStatsDTO(
                userRepository.count(), // total users or just employees? Frontend seems to label as "Total Employees"
                                        // but logic used users.length
                totalHR,
                totalMarketing,
                presentToday,
                onLeave,
                pendingLeaves,
                totalTeams,
                technicalTeamCount,
                totalAdmins));
    }
}
