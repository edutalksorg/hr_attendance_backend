package com.megamart.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userService.listAll());
    }

    @PostMapping("/approve/{targetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<User> approve(@PathVariable("targetId") UUID target,
                                        @RequestParam("role") UserRole role,
                                        @RequestParam("approverId") UUID approverId) {
        User u = userService.approveUser(approverId, target, role);
        return ResponseEntity.ok(u);
    }

    @PostMapping("/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> block(@PathVariable UUID id) {
        userService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/me")
    public ResponseEntity<User> me(@RequestHeader("Authorization") String auth) {
        // actual security filter will populate authentication; for simplicity, use SecurityContext in real flow
        return ResponseEntity.ok().build();
    }
}
