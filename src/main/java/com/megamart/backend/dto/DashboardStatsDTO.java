package com.megamart.backend.dto;

public record DashboardStatsDTO(
        long totalEmployees,
        long totalHR,
        long totalMarketing,
        long presentToday,
        long onLeave,
        long pendingLeaves,
        long totalTeams,
        long technicalTeamCount,
        long totalAdmins) {
}
