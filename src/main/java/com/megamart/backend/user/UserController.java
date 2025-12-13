package com.megamart.backend.user;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.lang.NonNull;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/users")
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserController {

    private final UserService userService;

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<List<User>> allUsers() {
        return ResponseEntity.ok(userService.listAll());
    }

    @GetMapping("/grouped")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<java.util.Map<String, List<User>>> groupedUsers() {
        return ResponseEntity.ok(userService.listAll().stream()
                .collect(java.util.stream.Collectors.groupingBy(u -> u.getRole().name())));
    }

    @PostMapping("/approve/{targetId}")
    @PreAuthorize("hasRole('ADMIN') or hasRole('HR')")
    public ResponseEntity<User> approve(@PathVariable("targetId") @NonNull UUID target,
            @RequestParam("approverId") @NonNull UUID approverId) {
        User u = userService.approveUser(approverId, target);
        return ResponseEntity.ok(u);
    }

    @PostMapping("/block/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<Void> block(@PathVariable @NonNull UUID id) {
        userService.blockUser(id);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/unblock/{id}")
    @PreAuthorize("hasRole('ADMIN')")
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

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE','MARKETING_EXECUTIVE')")
    public ResponseEntity<User> me() {
        String email = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication()
                .getName();
        User user = userService.findByEmail(email);
        return ResponseEntity.ok(user);
    }

    @GetMapping("/debug/authorities")
    public ResponseEntity<String> debugAuthorities() {
        var auth = org.springframework.security.core.context.SecurityContextHolder.getContext().getAuthentication();
        return ResponseEntity.ok("Authorities: " + auth.getAuthorities().toString() + ", Principal: " + auth.getName());
    }
}
