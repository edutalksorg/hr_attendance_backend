package com.megamart.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

@RestController
@RequestMapping("/api/v1/users")
@RequiredArgsConstructor
public class UserController {

    private static final Logger logger = LoggerFactory.getLogger(UserController.class);

    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<List<User>> allUsers() {
        try {
            return ResponseEntity.ok(userService.listAll());
        } catch (Exception e) {
            logger.error("Failed to fetch all users", e);
            throw new RuntimeException("Error fetching users: " + e.getMessage());
        }
    }

    @GetMapping("/grouped")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<java.util.Map<String, List<User>>> groupedUsers() {
        return ResponseEntity.ok(userService.listAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(u -> u.getRole().name())));
    }

    @GetMapping("/role/{role}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<User>> usersByRole(@PathVariable String role) {
        try {
            UserRole userRole = UserRole.valueOf(role.toUpperCase());
            return ResponseEntity.ok(userService.findByRole(userRole));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().build();
        }
    }

    @PostMapping("/approve/{targetId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<User> approve(@PathVariable("targetId") @NonNull UUID target) {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        User admin = userService.findByEmail(email);
        User u = userService.approveUser(admin.getId(), target);
        return ResponseEntity.ok(u);
    }

    @PostMapping("/block/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> block(@PathVariable @NonNull UUID id) {
        userService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unblock/{id}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> unblock(@PathVariable @NonNull UUID id) {
        userService.unblockUser(id);
        return ResponseEntity.ok().build();
    }

    @DeleteMapping("/delete/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> delete(@PathVariable @NonNull UUID id) {
        userService.deleteUser(id);
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/geo-restriction")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<User> updateGeoRestriction(
            @PathVariable @NonNull UUID id,
            @RequestBody com.megamart.backend.dto.GeoRestrictionRequest request) {
        return ResponseEntity.ok(userService.updateGeoRestriction(id, request));
    }

    @PutMapping("/bulk/geo-restriction")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<Void> bulkUpdateGeoRestriction(
            @RequestBody com.megamart.backend.dto.BulkGeoRestrictionRequest request) {
        userService.bulkUpdateGeoRestriction(request.getUserIds(), request.getData());
        return ResponseEntity.ok().build();
    }

    @PutMapping("/{id}/branch/{branchId}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<User> transferUser(@PathVariable UUID id, @PathVariable UUID branchId) {
        return ResponseEntity.ok(userService.transferUserToBranch(id, branchId));
    }

    @GetMapping("/branch/{branchId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<List<User>> usersByBranch(@PathVariable UUID branchId) {
        return ResponseEntity.ok(userService.findByBranch(branchId));
    }

    private final com.megamart.backend.profile.UserProfileService profileService;

    @GetMapping("/me")
    @PreAuthorize("isAuthenticated()")
    public ResponseEntity<User> me() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        User user = userService.findByEmail(email);

        // Populate profile data into User object
        try {
            com.megamart.backend.profile.UserProfileEntity profile = profileService.getProfile(user.getId());
            user.setProfilePhoto(profile.getPhotoUrl());
            user.setBio(profile.getBio());
            user.setUsername(profile.getUsername());
        } catch (Exception e) {
            // Ignore if profile load fails
        }

        return ResponseEntity.ok(user);
    }

    @GetMapping("/debug/authorities")
    public ResponseEntity<String> debugAuthorities() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok("Authorities: " + auth.getAuthorities().toString() + ", Principal: " + auth.getName());
    }

    @PutMapping("/{id}/joining-date")
    @PreAuthorize("hasAnyRole('ADMIN','HR','MANAGER')")
    public ResponseEntity<User> updateJoiningDate(@PathVariable UUID id,
            @RequestBody java.util.Map<String, String> payload) {
        String dateStr = payload.get("joiningDate");
        if (dateStr == null)
            return ResponseEntity.badRequest().build();
        java.time.LocalDate date = java.time.LocalDate.parse(dateStr); // Expects ISO-8601 YYYY-MM-DD
        return ResponseEntity.ok(userService.updateJoiningDate(id, date));
    }
}
