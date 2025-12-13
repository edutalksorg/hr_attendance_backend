package com.megamart.backend.admin;

import com.megamart.backend.attendance.Attendance;
import com.megamart.backend.attendance.AttendanceRepository;
import com.megamart.backend.navigation.NavigationLog;
import com.megamart.backend.navigation.NavigationRepository;
import com.megamart.backend.notification.Notification;
import com.megamart.backend.notification.NotificationRepository;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.*;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AttendanceRepository attendanceRepository;
    private final NavigationRepository navigationRepository;
    private final NotificationRepository notificationRepository;
    private final UserRepository userRepository;

    @GetMapping("/attendance/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Attendance>> attendanceForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(attendanceRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }

    @GetMapping("/attendance/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<List<Attendance>> allAttendance() {
        return ResponseEntity.ok(attendanceRepository.findAll());
    }

    @GetMapping("/attendance/hr")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Attendance>> hrTeamHistory() {
        List<User> hrUsers = userRepository.findByRole(com.megamart.backend.user.UserRole.HR);
        List<UUID> ids = hrUsers.stream().map(User::getId).collect(Collectors.toList());
        if (ids.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());
        return ResponseEntity.ok(attendanceRepository.findByUserIdInOrderByCreatedAtDesc(ids));
    }

    @GetMapping("/navigation/marketing")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<NavigationLog>> marketingNavigationHistory() {
        List<User> mUsers = userRepository.findByRole(com.megamart.backend.user.UserRole.MARKETING_EXECUTIVE);
        List<UUID> ids = mUsers.stream().map(User::getId).collect(Collectors.toList());
        if (ids.isEmpty())
            return ResponseEntity.ok(Collections.emptyList());
        return ResponseEntity.ok(navigationRepository.findByUserIdInOrderByCreatedAtDesc(ids));
    }

    @GetMapping("/notifications/user/{userId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<Notification>> notificationsForUser(@PathVariable UUID userId) {
        return ResponseEntity.ok(notificationRepository.findByUserIdOrderByCreatedAtDesc(userId));
    }
}
