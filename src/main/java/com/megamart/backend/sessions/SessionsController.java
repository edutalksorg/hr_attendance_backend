package com.megamart.backend.sessions;

import com.megamart.backend.auth.RefreshToken;
import com.megamart.backend.auth.RefreshTokenRepository;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/sessions")
@RequiredArgsConstructor
public class SessionsController {

    private final RefreshTokenRepository refreshTokenRepository;
    private final UserRepository userRepository;

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<List<SessionDto>> mySessions() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        List<SessionDto> list = refreshTokenRepository.findAll().stream()
                .filter(r -> r.getUser().getId().equals(user.getId()))
                .map(this::toDto)
                .collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    @PostMapping("/revoke")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<Void> revoke(@RequestParam String token) {
        // allow ADMIN to revoke any token; others can only revoke their own
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User requester = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));

        RefreshToken r = refreshTokenRepository.findByToken(token).orElseThrow(() -> new RuntimeException("Token not found"));
        if (requester.getRole() != com.megamart.backend.user.UserRole.ADMIN && !r.getUser().getId().equals(requester.getId())) {
            return ResponseEntity.status(403).build();
        }

        refreshTokenRepository.deleteByToken(token);
        return ResponseEntity.ok().build();
    }

    @GetMapping("/all")
    @PreAuthorize("hasRole('ADMIN')")
    public ResponseEntity<List<SessionDto>> allSessions() {
        List<SessionDto> list = refreshTokenRepository.findAll().stream().map(this::toDto).collect(Collectors.toList());
        return ResponseEntity.ok(list);
    }

    private SessionDto toDto(RefreshToken r) {
        return SessionDto.builder()
                .id(r.getId())
                .token(r.getToken())
                .createdAt(r.getCreatedAt())
                .expiresAt(r.getExpiresAt())
                .revoked(r.isRevoked())
                .build();
    }
}
