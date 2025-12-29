package com.megamart.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import java.time.OffsetDateTime;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AttendanceService {
    private final AttendanceRepository attendanceRepository;
    private final com.megamart.backend.user.UserRepository userRepository;
    private final com.fasterxml.jackson.databind.ObjectMapper objectMapper;

    public Attendance recordLogin(@NonNull UUID userId, String ip, String userAgent) {
        com.megamart.backend.user.User user = userRepository.findById(userId).orElseThrow();
        java.time.LocalTime now = java.time.LocalTime.now();

        String status = "Present";
        String shiftName = "Default";

        if (user.getShift() != null) {
            shiftName = user.getShift().getName();
            java.time.LocalTime start = user.getShift().getStartTime();
            int grace = user.getShift().getLateGraceMinutes() != null ? user.getShift().getLateGraceMinutes() : 15;
            if (now.isAfter(start.plusMinutes(grace))) {
                status = "Late";
            }
        } else {
            // Default 9:30 AM + 15 min grace = 9:45
            if (now.isAfter(java.time.LocalTime.of(9, 45))) {
                status = "Late";
            }
        }

        java.util.Map<String, Object> meta = new java.util.HashMap<>();
        meta.put("status", status);
        meta.put("shift", shiftName);

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

    public List<com.megamart.backend.dto.AttendanceHistoryDTO> getAttendanceHistoryLast60Days(UUID userId) {
        com.megamart.backend.user.User user = userRepository.findById(userId).orElse(null);
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
            }

            if (att != null) {
                status = "Present";
                remark = "Present";

                // Check metadata for status
                if (att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                    try {
                        java.util.Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                                new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                                });
                        if (meta != null && meta.containsKey("status")) {
                            status = (String) meta.get("status");
                            remark = status;
                        }
                    } catch (Exception e) {
                    }
                } else {
                    // Fallback using shift rules
                    java.time.LocalTime checkIn = att.getLoginTime().toLocalTime();

                    if (user != null && user.getShift() != null) {
                        try {
                            com.megamart.backend.shift.Shift s = user.getShift();
                            java.time.LocalTime shiftStart = s.getStartTime();
                            int grace = s.getLateGraceMinutes() != null ? s.getLateGraceMinutes() : 15;
                            java.time.LocalTime limit = shiftStart.plusMinutes(grace);

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
                        if (checkIn.isAfter(java.time.LocalTime.of(9, 45))) {
                            status = "Late";
                            remark = "Late Arrival";
                        }
                    }
                }
            }

            java.util.List<java.util.Map<String, String>> ipHistory = null;
            if (att != null && att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                try {
                    java.util.Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                    if (meta != null && meta.containsKey("ipHistory")) {
                        Object historyObj = meta.get("ipHistory");
                        if (historyObj instanceof java.util.List) {
                            @SuppressWarnings("unchecked")
                            java.util.List<java.util.Map<String, String>> casted = (java.util.List<java.util.Map<String, String>>) historyObj;
                            ipHistory = casted;
                        }
                    }
                } catch (Exception e) {
                }
            }

            history.add(com.megamart.backend.dto.AttendanceHistoryDTO.builder()
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
        List<com.megamart.backend.dto.AttendanceHistoryDTO> history = getAttendanceHistoryLast60Days(userId);

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

    public com.megamart.backend.dto.AttendanceHistoryDTO getByDate(UUID userId, java.time.LocalDate date) {
        OffsetDateTime start = date.atStartOfDay().atOffset(OffsetDateTime.now().getOffset());
        OffsetDateTime end = date.plusDays(1).atStartOfDay().atOffset(OffsetDateTime.now().getOffset());

        List<Attendance> records = attendanceRepository.findByUserIdAndLoginTimeBetweenOrderByLoginTimeDesc(userId,
                start, end);
        Attendance att = records.isEmpty() ? null : records.get(0);
        com.megamart.backend.user.User user = userRepository.findById(userId).orElse(null);

        String status = "Absent";
        String remark = "No Data";

        if (att != null) { // Logic duplication for single record
            status = "Present";
            remark = "Present";

            if (att.getMetadata() != null && !att.getMetadata().isEmpty()) {
                try {
                    java.util.Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                    if (meta != null && meta.containsKey("status")) {
                        status = (String) meta.get("status");
                        remark = status;
                    }
                } catch (Exception e) {
                }
            } else {
                java.time.LocalTime checkIn = att.getLoginTime().toLocalTime();
                java.time.LocalTime limit = java.time.LocalTime.of(9, 45);
                if (user != null && user.getShift() != null) {
                    limit = user.getShift().getStartTime().plusMinutes(
                            user.getShift().getLateGraceMinutes() != null ? user.getShift().getLateGraceMinutes() : 15);
                }
                if (checkIn.isAfter(limit)) {
                    status = "Late";
                    remark = "Late Arrival";
                }
            }
        } else if (date.getDayOfWeek() == java.time.DayOfWeek.SUNDAY) {
            status = "Holiday";
            remark = "Sunday Holiday";
        }

        java.util.List<java.util.Map<String, String>> ipHistory = null;
        if (att != null && att.getMetadata() != null && !att.getMetadata().isEmpty()) {
            try {
                java.util.Map<String, Object> meta = objectMapper.readValue(att.getMetadata(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
                if (meta != null && meta.containsKey("ipHistory")) {
                    Object historyObj = meta.get("ipHistory");
                    if (historyObj instanceof java.util.List) {
                        @SuppressWarnings("unchecked")
                        java.util.List<java.util.Map<String, String>> casted = (java.util.List<java.util.Map<String, String>>) historyObj;
                        ipHistory = casted;
                    }
                }
            } catch (Exception e) {
            }
        }

        return com.megamart.backend.dto.AttendanceHistoryDTO.builder()
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

    // Removed objectMapper field since it's now injected at class level

    public void recordHourlyIp(UUID userId, String ip) {
        Attendance a = attendanceRepository.findTopByUserIdAndLogoutTimeIsNullOrderByLoginTimeDesc(userId)
                .orElse(null);
        if (a == null)
            return;

        try {
            java.util.Map<String, Object> meta;
            if (a.getMetadata() == null || a.getMetadata().isEmpty()) {
                meta = new java.util.HashMap<>();
            } else {
                meta = objectMapper.readValue(a.getMetadata(),
                        new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                        });
            }

            @SuppressWarnings("unchecked")
            java.util.List<java.util.Map<String, String>> history = (java.util.List<java.util.Map<String, String>>) meta
                    .getOrDefault("ipHistory", new java.util.ArrayList<>());

            if (!history.isEmpty()) {
                java.util.Map<String, String> last = history.get(history.size() - 1);
                String lastTs = last.get("timestamp");
                try {
                    OffsetDateTime lastTime = OffsetDateTime.parse(lastTs);
                    if (java.time.Duration.between(lastTime, OffsetDateTime.now()).toMinutes() < 120) {
                        String lastIp = last.get("ip");
                        if (lastIp != null && lastIp.equals(ip)) {
                            return;
                        }
                    }
                } catch (Exception e) {
                }
            }

            java.util.Map<String, String> entry = new java.util.HashMap<>();
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

    public Attendance updateAttendance(UUID id, com.megamart.backend.dto.UpdateAttendanceRequest req) {
        Attendance a = attendanceRepository.findById(id)
                .orElseThrow(() -> new RuntimeException("Attendance not found"));

        if (req.getStatus() != null) {
            // Update status in metadata
            try {
                java.util.Map<String, Object> meta;
                if (a.getMetadata() != null && !a.getMetadata().isEmpty()) {
                    meta = objectMapper.readValue(a.getMetadata(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                } else {
                    meta = new java.util.HashMap<>();
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

        // If remarks are needed, they can be stored in metadata or a new field.
        // For now, let's store remarks in metadata too if provided
        if (req.getRemark() != null) {
            try {
                java.util.Map<String, Object> meta;
                if (a.getMetadata() != null && !a.getMetadata().isEmpty()) {
                    meta = objectMapper.readValue(a.getMetadata(),
                            new com.fasterxml.jackson.core.type.TypeReference<java.util.Map<String, Object>>() {
                            });
                } else {
                    meta = new java.util.HashMap<>();
                }
                meta.put("remark", req.getRemark());
                a.setMetadata(objectMapper.writeValueAsString(meta));
            } catch (Exception e) {
            }
        }

        return attendanceRepository.save(a);
    }

    public Attendance createManualAttendance(UUID userId, com.megamart.backend.dto.UpdateAttendanceRequest req) {
        // If checkIn is provided, check efficiently for duplicates on that day?
        // For now, simplify: just create it. The user intends to add a record.
        // Or if ID is passed? No, this is for new creation.

        OffsetDateTime checkIn = req.getCheckIn() != null ? req.getCheckIn() : OffsetDateTime.now(); // Fallback if
                                                                                                     // missing, but
                                                                                                     // should be there

        java.util.Map<String, Object> meta = new java.util.HashMap<>();
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
