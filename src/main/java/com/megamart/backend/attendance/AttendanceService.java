package com.megamart.backend.attendance;

import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import java.time.LocalTime;
import java.time.LocalDate;
import java.time.Duration;
import java.time.DayOfWeek;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;
import java.util.Map;
import java.util.HashMap;
import java.util.ArrayList;
import java.util.stream.Collectors;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.core.type.TypeReference;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.dto.AttendanceHistoryDTO;
import com.megamart.backend.dto.UpdateAttendanceRequest;
import com.megamart.backend.shift.Shift;

@Service
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final UserRepository userRepository;
    private final ObjectMapper objectMapper;

    public AttendanceService(AttendanceRepository attendanceRepository,
            UserRepository userRepository,
            ObjectMapper objectMapper) {
        this.attendanceRepository = attendanceRepository;
        this.userRepository = userRepository;
        this.objectMapper = objectMapper;
    }

    public Attendance recordLogin(@NonNull UUID userId, String ip, String userAgent, Double lat, Double lng) {
        User user = userRepository.findById(userId).orElseThrow();

        // --- Geolocation Enforcement ---
        if (Boolean.TRUE.equals(user.getGeoRestrictionEnabled())) {
            if (lat == null || lng == null) {
                throw new RuntimeException("Geolocation required: Signal loss detected.");
            }

            Double targetLat = user.getOfficeLatitude();
            Double targetLng = user.getOfficeLongitude();
            Double targetRadius = user.getGeoRadius();

            // Branch Level Fallback: If individual coordinates are missing, use branch
            // rules
            if (targetLat == null && user.getBranch() != null) {
                targetLat = user.getBranch().getLatitude();
                targetLng = user.getBranch().getLongitude();
                if (targetRadius == null || targetRadius == 50.0) { // Default radius override
                    targetRadius = user.getBranch().getGeoRadius();
                }
            }

            double officeLat = targetLat != null ? targetLat : 0.0;
            double officeLng = targetLng != null ? targetLng : 0.0;
            double radius = targetRadius != null ? targetRadius : 100.0;

            double distance = calculateDistance(lat, lng, officeLat, officeLng);

            if (distance > radius) {
                throw new RuntimeException("Vector Breach: You are outside your authorized biometric perimeter.");
            }
        }

        LocalTime now = LocalTime.now();

        String status = "Present";
        String shiftName = "Default";

        if (user.getShift() != null) {
            shiftName = user.getShift().getName();
            LocalTime start = user.getShift().getStartTime();
            int grace = user.getShift().getLateGraceMinutes() != null ? user.getShift().getLateGraceMinutes() : 15;
            if (now.isAfter(start.plusMinutes(grace))) {
                status = "Late";
            }
        } else {
            // Default 9:30 AM + 15 min grace = 9:45
            if (now.isAfter(LocalTime.of(9, 45))) {
                status = "Late";
            }
        }

        Map<String, Object> meta = new HashMap<>();
        meta.put("status", status);
        meta.put("shift", shiftName);
        if (lat != null)
            meta.put("lat", lat);
        if (lng != null)
            meta.put("lng", lng);

        String metaJson;
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
            metaJson = "{}";
        }

        Attendance a = Attendance.builder()
                .userId(userId)
                .loginTime(OffsetDateTime.now())
                .ipAddress(ip)
                .userAgent(userAgent)
                .createdAt(OffsetDateTime.now())
                .metadata(metaJson)
                .build();
        return attendanceRepository.save(a);
    }

    private double calculateDistance(double lat1, double lon1, double lat2, double lon2) {
        final int R = 6371; // Earth radius in km
        double latDistance = Math.toRadians(lat2 - lat1);
        double lonDistance = Math.toRadians(lon2 - lon1);
        double a = Math.sin(latDistance / 2) * Math.sin(latDistance / 2)
                + Math.cos(Math.toRadians(lat1)) * Math.cos(Math.toRadians(lat2))
                        * Math.sin(lonDistance / 2) * Math.sin(lonDistance / 2);
        double c = 2 * Math.atan2(Math.sqrt(a), Math.sqrt(1 - a));
        return R * c * 1000; // Distance in meters
    }

    public Attendance recordLogout(@NonNull UUID attendanceId, String ipAddress) {
        Attendance a = attendanceRepository.findById(attendanceId).orElseThrow();
        a.setLogoutTime(OffsetDateTime.now());
        if (ipAddress != null && !ipAddress.isEmpty()) {
            a.setLogoutIpAddress(ipAddress);
        }
        return attendanceRepository.save(a);
    }

    public List<Attendance> getHistory(UUID userId) {
        return attendanceRepository.findByUserIdOrderByCreatedAtDesc(userId);
    }

    private static final int RETENTION_DAYS = 60;

    public List<AttendanceHistoryDTO> getAttendanceHistoryLast60Days(UUID userId) {
        User user = userRepository.findById(userId).orElse(null);
        OffsetDateTime end = OffsetDateTime.now();
        OffsetDateTime start = end.minusDays(RETENTION_DAYS);

        List<Attendance> records = attendanceRepository.findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId,
                start, end);
        List<AttendanceHistoryDTO> history = new ArrayList<>();

        Map<LocalDate, Attendance> attendanceMap = records.stream()
                .collect(Collectors.toMap(
                        a -> a.getLoginTime().toLocalDate(),
                        a -> a,
                        (existing, replacement) -> existing));

        for (int i = 0; i < RETENTION_DAYS; i++) {
            LocalDate date = end.minusDays(i).toLocalDate();
            Attendance att = attendanceMap.get(date);

            String status = "Absent";
            String remark = "Absent";

            if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
                status = "Holiday";
                remark = "Sunday Holiday";
            }

            if (att != null) {
                status = "Present";
                remark = "Present";

                // Check metadata for status
                if (att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                    try {
                        Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                                new TypeReference<Map<String, Object>>() {
                                });
                        if (meta != null && meta.containsKey("status")) {
                            status = (String) meta.get("status");
                            remark = status;
                        }
                    } catch (Exception e) {
                    }
                } else {
                    // Fallback using shift rules
                    LocalTime checkIn = att.getLoginTime().toLocalTime();

                    if (user != null && user.getShift() != null) {
                        try {
                            Shift s = user.getShift();
                            LocalTime shiftStart = s.getStartTime();
                            int grace = s.getLateGraceMinutes() != null ? s.getLateGraceMinutes() : 15;
                            LocalTime limit = shiftStart.plusMinutes(grace);

                            if (checkIn.isAfter(limit)) {
                                status = "Late";
                                remark = "Late Arrival";
                            }

                            // Check for Absent Time Rule (if check-in is VERY late)
                            if (s.getAbsentTime() != null && checkIn.isAfter(s.getAbsentTime())) {
                                status = "Absent";
                                remark = "Marked Absent (Late Check-in)";
                            }

                            // Check for Half Day (if check-out exists)
                            if (att.getLogoutTime() != null && s.getHalfDayTime() != null) {
                                if (att.getLogoutTime().toLocalTime().isBefore(s.getHalfDayTime())) {
                                    status = "Half Day";
                                    remark = "Left Early";
                                }
                            }
                        } catch (Exception e) {
                        }
                    } else {
                        // Default Rules
                        if (checkIn.isAfter(LocalTime.of(9, 45))) {
                            status = "Late";
                            remark = "Late Arrival";
                        }
                    }
                }
            }

            List<Map<String, String>> ipHistory = null;
            if (att != null && att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                try {
                    Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    if (meta != null && meta.containsKey("ipHistory")) {
                        Object historyObj = meta.get("ipHistory");
                        if (historyObj instanceof List) {
                            @SuppressWarnings("unchecked")
                            List<Map<String, String>> casted = (List<Map<String, String>>) historyObj;
                            ipHistory = casted;
                        }
                    }
                } catch (Exception e) {
                }
            }

            history.add(AttendanceHistoryDTO.builder()
                    .id(att != null ? att.getId() : null)
                    .date(date)
                    .checkIn(att != null ? att.getLoginTime() : null)
                    .checkOut(att != null ? att.getLogoutTime() : null)
                    .ipAddress(att != null ? att.getIpAddress() : null)
                    .logoutIpAddress(att != null ? att.getLogoutIpAddress() : null)
                    .status(status)
                    .remark(remark)
                    .canCheckOut(att != null ? att.getCanCheckOut() : false)
                    .ipHistory(ipHistory)
                    .build());
        }
        return history;
    }

    public static record AttendanceStatsDTO(long totalDays, long presentDays, long lateDays, double attendanceRate) {
    }

    public List<Attendance> listAll() {
        return attendanceRepository.findAll();
    }

    public AttendanceStatsDTO getStats(UUID userId) {
        List<AttendanceHistoryDTO> history = getAttendanceHistoryLast60Days(userId);

        long workingDays = history.stream()
                .filter(d -> !d.getStatus().contains("Holiday"))
                .count();

        long present = history.stream()
                .filter(d -> "Present".equals(d.getStatus()) || "Late".equals(d.getStatus())
                        || "Active".equals(d.getStatus()))
                .count();

        long late = history.stream()
                .filter(d -> "Late".equals(d.getStatus()))
                .count();

        double rate = workingDays > 0 ? ((double) present / workingDays) * 100.0 : 0.0;

        return new AttendanceStatsDTO(workingDays, present, late, rate);
    }

    public AttendanceHistoryDTO getByDate(UUID userId, LocalDate date) {
        OffsetDateTime start = date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime end = date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        List<Attendance> records = attendanceRepository.findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId,
                start, end);
        Attendance att = records.isEmpty() ? null : records.get(0);
        User user = userRepository.findById(userId).orElse(null);

        String status = "Absent";
        String remark = "No Data";

        if (att != null) { // Logic duplication for single record
            status = "Present";
            remark = "Present";

            if (att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                try {
                    Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                            new TypeReference<Map<String, Object>>() {
                            });
                    if (meta != null && meta.containsKey("status")) {
                        status = (String) meta.get("status");
                        remark = status;
                    }
                } catch (Exception e) {
                }
            } else {
                LocalTime checkIn = att.getLoginTime().toLocalTime();
                LocalTime limit = LocalTime.of(9, 45);
                if (user != null && user.getShift() != null) {
                    limit = user.getShift().getStartTime().plusMinutes(
                            user.getShift().getLateGraceMinutes() != null ? user.getShift().getLateGraceMinutes() : 15);
                }
                if (checkIn.isAfter(limit)) {
                    status = "Late";
                    remark = "Late Arrival";
                }
            }
        } else if (date.getDayOfWeek() == DayOfWeek.SUNDAY) {
            status = "Holiday";
            remark = "Sunday Holiday";
        }

        List<Map<String, String>> ipHistory = null;
        if (att != null && att.getMetadata() != null && !att.getMetadata().isEmpty()) {
            try {
                Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                        new TypeReference<Map<String, Object>>() {
                        });
                if (meta != null && meta.containsKey("ipHistory")) {
                    Object historyObj = meta.get("ipHistory");
                    if (historyObj instanceof List) {
                        @SuppressWarnings("unchecked")
                        List<Map<String, String>> casted = (List<Map<String, String>>) historyObj;
                        ipHistory = casted;
                    }
                }
            } catch (Exception e) {
            }
        }

        return AttendanceHistoryDTO.builder()
                .id(att != null ? att.getId() : null)
                .date(date)
                .checkIn(att != null ? att.getLoginTime() : null)
                .checkOut(att != null ? att.getLogoutTime() : null)
                .ipAddress(att != null ? att.getIpAddress() : null)
                .logoutIpAddress(att != null ? att.getLogoutIpAddress() : null)
                .status(status)
                .remark(remark)
                .canCheckOut(att != null ? att.getCanCheckOut() : false)
                .ipHistory(ipHistory)
                .build();
    }

    public void recordHourlyIp(UUID userId, String ip) {
        Attendance a = attendanceRepository.findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId)
                .orElse(null);
        if (a == null)
            return;

        try {
            Map<String, Object> meta;
            if (a.getMetadata() == null || a.getMetadata().isEmpty()) {
                meta = new HashMap<>();
            } else {
                meta = objectMapper.readValue(a.getMetadata(),
                        new TypeReference<Map<String, Object>>() {
                        });
            }

            @SuppressWarnings("unchecked")
            List<Map<String, String>> history = (List<Map<String, String>>) meta
                    .getOrDefault("ipHistory", new ArrayList<>());

            if (!history.isEmpty()) {
                Map<String, String> last = history.get(history.size() - 1);
                String lastTs = last.get("timestamp");
                try {
                    OffsetDateTime lastTime = OffsetDateTime.parse(lastTs);
                    if (Duration.between(lastTime, OffsetDateTime.now()).toMinutes() < 120) {
                        String lastIp = last.get("ip");
                        if (lastIp != null && lastIp.equals(ip)) {
                            return;
                        }
                    }
                } catch (Exception e) {
                }
            }

            Map<String, String> entry = new HashMap<>();
            entry.put("timestamp", OffsetDateTime.now().toString());
            entry.put("ip", ip);
            history.add(entry);

            meta.put("ipHistory", history);
            a.setMetadata(objectMapper.writeValueAsString(meta));
            attendanceRepository.save(a);
        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public Attendance updateAttendance(UUID id, UpdateAttendanceRequest req) {
        Attendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        if (req.getStatus() != null) {
            // Update status in metadata
            try {
                Map<String, Object> meta;
                if (a.getMetadata() != null && !a.getMetadata().isEmpty()) {
                    meta = objectMapper.readValue(a.getMetadata(),
                            new TypeReference<Map<String, Object>>() {
                            });
                } else {
                    meta = new HashMap<>();
                }
                meta.put("status", req.getStatus());
                a.setMetadata(objectMapper.writeValueAsString(meta));
            } catch (Exception e) {
                // Ignore
            }
        }

        if (req.getCheckIn() != null) {
            a.setLoginTime(req.getCheckIn());
        }
        if (req.getCheckOut() != null) {
            a.setLogoutTime(req.getCheckOut());
        }

        if (req.getRemark() != null) {
            try {
                Map<String, Object> meta;
                if (a.getMetadata() != null && !a.getMetadata().isEmpty()) {
                    meta = objectMapper.readValue(a.getMetadata(),
                            new TypeReference<Map<String, Object>>() {
                            });
                } else {
                    meta = new HashMap<>();
                }
                meta.put("remark", req.getRemark());
                a.setMetadata(objectMapper.writeValueAsString(meta));
            } catch (Exception e) {
            }
        }

        return attendanceRepository.save(a);
    }

    public Attendance createManualAttendance(UUID userId, UpdateAttendanceRequest req) {
        OffsetDateTime checkIn = req.getCheckIn() != null ? req.getCheckIn() : OffsetDateTime.now();

        Map<String, Object> meta = new HashMap<>();
        if (req.getStatus() != null)
            meta.put("status", req.getStatus());
        if (req.getRemark() != null)
            meta.put("remark", req.getRemark());

        String metaJson = "{}";
        try {
            metaJson = objectMapper.writeValueAsString(meta);
        } catch (Exception e) {
        }

        Attendance a = Attendance.builder()
                .userId(userId)
                .loginTime(checkIn)
                .logoutTime(req.getCheckOut())
                .metadata(metaJson)
                .createdAt(OffsetDateTime.now())
                .ipAddress("Manual Entry")
                .build();

        return attendanceRepository.save(a);
    }
}
