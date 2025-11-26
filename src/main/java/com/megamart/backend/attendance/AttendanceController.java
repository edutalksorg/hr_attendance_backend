package com.megamart.backend.attendance;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/attendance")
@RequiredArgsConstructor
public class AttendanceController {

    private final AttendanceService attendanceService;

    @PostMapping("/login/{userId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','ADMIN')")
    public ResponseEntity<Attendance> login(@PathVariable UUID userId, @RequestHeader(value="X-User-Agent", required=false) String ua, @RequestHeader(value="X-Forwarded-For", required=false) String ip) {
        String ipAddr = (ip == null) ? "unknown" : ip;
        Attendance a = attendanceService.recordLogin(userId, ipAddr, ua);
        return ResponseEntity.ok(a);
    }

    @PostMapping("/logout/{attendanceId}")
    @PreAuthorize("hasAnyRole('EMPLOYEE','HR','ADMIN')")
    public ResponseEntity<Attendance> logout(@PathVariable UUID attendanceId) {
        return ResponseEntity.ok(attendanceService.recordLogout(attendanceId));
    }

    @GetMapping("/history/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<Attendance>> history(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceService.getHistory(userId));
    }
}
