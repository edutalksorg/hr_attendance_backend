package com.megamart.backend.navigation;

import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/navigation")
@RequiredArgsConstructor
public class NavigationController {
    private final NavigationService service;

    @PostMapping("/log")
    @PreAuthorize("hasRole('MARKETING_EXECUTIVE')")
    public ResponseEntity<NavigationLog> log(@RequestParam UUID userId, @RequestParam String path, @RequestParam(required=false) String metadata) {
        return ResponseEntity.ok(service.log(userId, path, metadata));
    }

    @GetMapping("/history/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR')")
    public ResponseEntity<List<NavigationLog>> history(@PathVariable UUID userId) {
        return ResponseEntity.ok(service.history(userId));
    }
}
