package com.megamart.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;

    public Attendance recordLogin(@NonNull UUID userId, String ip, String userAgent) {
        Attendance a = Attendance.builder()
                .userId(userId)
                .loginTime(OffsetDateTime.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .createdAt(OffsetDateTime.now())
                .build();
        return attendanceRepository.save(a);
    }

    public Attendance recordLogout(@NonNull UUID attendanceId) {
        Attendance a = attendanceRepository.findById(attendanceId).orElseThrow();
        a.setLogoutTime(OffsetDateTime.now());
        return attendanceRepository.save(a);
    }

    public List<Attendance> getHistory(UUID userId) {
        return attendanceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private static final int RETENTION_DAYS = 60;

    public List<com.megamart.backend.dto.AttendanceHistoryDTO> getAttendanceHistoryLast60Days(UUID userId) {
        OffsetDateTime end = OffsetDateTime.now();
        OffsetDateTime start = end.minusDays(RETENTION_DAYS);

        List<Attendance> records = attendanceRepository.findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId,
                start, end);
        List<com.megamart.backend.dto.AttendanceHistoryDTO> history = new java.util.ArrayList<>();

        java.util.Map<java.time.LocalDate, Attendance> attendanceMap = records.stream()
                .collect(java.util.stream.Collectors.toMap(
                        a -> a.getLoginTime().toLocalDate(),
                        a -> a,
                        (existing, replacement) -> existing));

        for (int i = 0; i < RETENTION_DAYS; i++) {
            java.time.LocalDate date = end.minusDays(i).toLocalDate();
            Attendance att = attendanceMap.get(date);

            String status = "Absent";
            String remark = "Absent";

            if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
                status = "Holiday";
                remark = "Sunday Holiday";
            } else if (att != null) {
                status = "Present";
                remark = "Present";
            }

            history.add(com.megamart.backend.dto.AttendanceHistoryDTO.builder()
                    .date(date)
                    .checkIn(att != null ? att.getLoginTime() : null)
                    .checkOut(att != null ? att.getLogoutTime() : null)
                    .ipAddress(att != null ? att.getIpAddress() : null)
                    .status(status)
                    .remark(remark)
                    .build());
        }
        return history;
    }

    @org.springframework.scheduling.annotation.Scheduled(cron = "0 0 0 * * *")
    @org.springframework.transaction.annotation.Transactional
    public void cleanupOldRecords() {
        OffsetDateTime threshold = OffsetDateTime.now().minusDays(RETENTION_DAYS);
        attendanceRepository.deleteByCreatedAtBefore(threshold);
    }
}
