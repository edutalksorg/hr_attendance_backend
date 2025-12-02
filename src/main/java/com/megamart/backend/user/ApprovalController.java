package com.megamart.backend.user;

import com.megamart.backend.auth.ApiResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class ApprovalController {

    private final UserRepository userRepository;
    private final UserService userService;

    @PostMapping("/approve/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<ApiResponse> approveUser(@PathVariable("userId") UUID userId,
                                                   @RequestParam(name = "role") String roleStr) {

        String approverEmail = SecurityContextHolder.getContext().getAuthentication().getName();
        User approver = userRepository.findByEmail(approverEmail)
                .orElseThrow(() -> new RuntimeException("Approver not found"));

        UserRole roleToSet;
        try {
            roleToSet = UserRole.valueOf(roleStr.toUpperCase());
        } catch (IllegalArgumentException ex) {
            return ResponseEntity.badRequest().body(new ApiResponse("Invalid role to approve"));
        }

        // Business rules: use UserService.canApprove
        if (!userService.canApprove(approver.getRole(), roleToSet)) {
            return ResponseEntity.status(HttpStatus.FORBIDDEN)
                    .body(new ApiResponse("You are not authorized to approve this role"));
        }

        userService.approveUser(approver.getId(), userId, roleToSet);

        return ResponseEntity.ok(new ApiResponse("User approved successfully"));
    }
}
