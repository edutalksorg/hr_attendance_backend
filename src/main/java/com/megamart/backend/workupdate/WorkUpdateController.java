package com.megamart.backend.workupdate;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRole;
import com.megamart.backend.user.UserService;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/v1/work-updates")
@RequiredArgsConstructor
public class WorkUpdateController {

    private final WorkUpdateService workUpdateService;
    private final UserService userService;

    private User getCurrentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userService.findByEmail(email);
    }

    @PostMapping("/today")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<WorkUpdate> submitTodayUpdate(@RequestBody UpdateRequest request) {
        User user = getCurrentUser();
        return ResponseEntity.ok(workUpdateService.createWorkUpdate(user, request.getDescription()));
    }

    @GetMapping("/my/today")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<WorkUpdate> getMyTodayUpdate() {
        return ResponseEntity.ok(workUpdateService.getTodayUpdate(getCurrentUser()));
    }

    @GetMapping("/my/history")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<WorkUpdate>> getMyHistory() {
        return ResponseEntity.ok(workUpdateService.getMyUpdates(getCurrentUser()));
    }

    @GetMapping
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<WorkUpdate>> getAllUpdates(
            @RequestParam(required = false) UUID userId,
            @RequestParam(required = false) Integer month,
            @RequestParam(required = false) Integer year,
            @RequestParam(required = false) UUID branchId,
            @RequestParam(required = false) String role) {

        UserRole userRole = role != null ? UserRole.valueOf(role.toUpperCase()) : null;
        return ResponseEntity.ok(workUpdateService.getAllUpdates(userId, month, year, branchId, userRole));
    }

    @DeleteMapping("/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> deleteUpdate(@PathVariable UUID id) {
        workUpdateService.deleteUpdate(id, getCurrentUser());
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/bulk")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> deleteBulk(@RequestBody List<UUID> ids) {
        workUpdateService.deleteBulk(ids, getCurrentUser());
        return ResponseEntity.ok().build();
    }

    @lombok.Data
    public static class UpdateRequest {
        private String description;
    }
}
