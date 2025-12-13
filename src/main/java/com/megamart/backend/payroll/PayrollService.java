package com.megamart.backend.payroll;

import com.megamart.backend.attendance.Attendance;
import com.megamart.backend.attendance.AttendanceRepository;
import com.megamart.backend.holidays.HolidayRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.Duration;
import java.time.LocalDate;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class PayrollService {
    private final AttendanceRepository attendanceRepository;
    private final HolidayRepository holidayRepository;

    /**
     * Calculate attendance-based payroll data between two dates (inclusive)
     */
    public PayrollDto calculate(UUID userId, LocalDate start, LocalDate end) {
        // fetch attendance records and filter by date range
        List<Attendance> all = attendanceRepository.findByUserIdOrderByCreatedAtDesc(userId);
        List<Attendance> inRange = all.stream().filter(a -> {
            OffsetDateTime l = a.getLoginTime();
            if (l == null) return false;
            LocalDate ld = l.toLocalDate();
            return (ld.isEqual(start) || ld.isAfter(start)) && (ld.isEqual(end) || ld.isBefore(end));
        }).collect(Collectors.toList());

        double totalHours = 0.0;
        long daysPresent = 0;
        for (Attendance a : inRange) {
            if (a.getLoginTime() != null && a.getLogoutTime() != null) {
                Duration d = Duration.between(a.getLoginTime(), a.getLogoutTime());
                totalHours += d.toMinutes() / 60.0;
                daysPresent++;
            }
        }

        long holidayCount = holidayRepository.findByHolidayDateBetween(start, end).size();

        long totalDays = java.time.temporal.ChronoUnit.DAYS.between(start, end) + 1;
        long absentDays = Math.max(0, totalDays - daysPresent - holidayCount);

        double avg = daysPresent > 0 ? totalHours / daysPresent : 0.0;

        return PayrollDto.builder()
                .daysPresent(daysPresent)
                .totalHours(Math.round(totalHours * 100.0) / 100.0)
                .averageHoursPerDay(Math.round(avg * 100.0) / 100.0)
                .holidayCount(holidayCount)
                .absentDays(absentDays)
                .build();
    }
}
