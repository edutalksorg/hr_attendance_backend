package com.megamart.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;

    public Attendance recordLogin(UUID userId, String ip, String userAgent) {
        Attendance a = Attendance.builder()
                .userId(userId)
                .loginTime(OffsetDateTime.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .createdAt(OffsetDateTime.now())
                .build();
        return attendanceRepository.save(a);
    }

    public Attendance recordLogout(UUID attendanceId) {
        Attendance a = attendanceRepository.findById(attendanceId).orElseThrow();
        a.setLogoutTime(OffsetDateTime.now());
        return attendanceRepository.save(a);
    }

    public List<Attendance> getHistory(UUID userId) { return attendanceRepository.findByUserIdOrderByCreatedAtDesc(userId); }
}
