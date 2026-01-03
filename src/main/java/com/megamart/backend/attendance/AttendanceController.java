package com.megamart.backend.attendance;

import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/attendance")
public class AttendanceController {

    private final AttendanceService attendanceService;
    private final com.megamart.backend.security.IpDetectionService ipService;

    public AttendanceController(AttendanceService attendanceService,
            com.megamart.backend.security.IpDetectionService ipService) {
        this.attendanceService = attendanceService;
        this.ipService = ipService;
    }

    @PostMapping("/login/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','ADMIN','MANAGER','MARKETING_EXECUTIVE')")
    public ResponseEntity<Attendance> login(@PathVariable UUID userId,
            @RequestParam(required = false) Double lat,
            @RequestParam(required = false) Double lng,
            @RequestHeader(value = "X-User-Agent", required = false) String ua,
            jakarta.servlet.http.HttpServletRequest request) {
        String ipAddr = ipService.getClientIp(request);
        Attendance a = attendanceService.recordLogin(userId, ipAddr, ua, lat, lng);
        return ResponseEntity.ok(a);
    }

    @PostMapping("/logout/{attendanceId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','ADMIN','MANAGER','MARKETING_EXECUTIVE')")
    public ResponseEntity<Attendance> logout(@PathVariable UUID attendanceId,
            jakarta.servlet.http.HttpServletRequest request) {
        String ipAddr = ipService.getClientIp(request);
        return ResponseEntity.ok(attendanceService.recordLogout(attendanceId, ipAddr));
    }

    @PutMapping("/update/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Attendance> updateAttendance(
            @PathVariable UUID id,
            @RequestBody com.megamart.backend.dto.UpdateAttendanceRequest request) {
        return ResponseEntity.ok(attendanceService.updateAttendance(id, request));
    }

    @PostMapping("/manual")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Attendance> createManual(
            @RequestBody com.megamart.backend.dto.UpdateAttendanceRequest request) {
        if (request.getUserId() == null) {
            return ResponseEntity.badRequest().build();
        }
        return ResponseEntity.ok(attendanceService.createManualAttendance(request.getUserId(), request));
    }

    @PostMapping("/track/{userId}")
    @PreAuthorize("hasAnyRole('MARKETING_EXECUTIVE')")
    public ResponseEntity<Void> trackSession(@PathVariable UUID userId,
            jakarta.servlet.http.HttpServletRequest request) {
        String ip = ipService.getClientIp(request);
        attendanceService.recordHourlyIp(userId, ip);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/history/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<Attendance>> history(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getHistory(userId));
    }

    @GetMapping("/history/60days/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<com.megamart.backend.dto.AttendanceHistoryDTO>> history60Days(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistoryLast60Days(userId));
    }

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<Attendance>> all() {
        return ResponseEntity.ok(attendanceService.listAll());
    }

    @GetMapping("/stats/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<AttendanceService.AttendanceStatsDTO> stats(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getStats(userId));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<com.megamart.backend.dto.AttendanceHistoryDTO> getByDate(
            @RequestParam UUID userId,
            @RequestParam java.time.LocalDate date) {
        return ResponseEntity.ok(attendanceService.getByDate(userId, date));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<com.megamart.backend.dto.AttendanceHistoryDTO>> historyByUserId(
            @PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getAttendanceHistoryLast60Days(userId));
    }
}
