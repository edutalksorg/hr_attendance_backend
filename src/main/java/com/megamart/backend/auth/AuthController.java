package com.megamart.backend.auth;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.server.ResponseStatusException;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    @PostMapping("/register")
    public ResponseEntity<ApiResponse> register(@Valid @RequestBody RegisterRequest req) {
        authService.register(req);
        return ResponseEntity.ok(new ApiResponse("User registered successfully — waiting for approval"));
    }

    @PostMapping("/login")
    public ResponseEntity<?> login(@Valid @RequestBody AuthRequest req, HttpServletRequest request) {
        String ip = request.getRemoteAddr();
        try {
            AuthResponse resp = authService.login(req, ip);
            return ResponseEntity.ok(resp);
        } catch (ResponseStatusException ex) {
            if (ex.getStatusCode() == HttpStatus.UNAUTHORIZED) {
                return ResponseEntity.status(HttpStatus.UNAUTHORIZED)
                        .body(new ApiResponse(ex.getReason()));
            }
            if (ex.getStatusCode() == HttpStatus.FORBIDDEN) {
                return ResponseEntity.status(HttpStatus.FORBIDDEN)
                        .body(new ApiResponse(ex.getReason()));
            }
            throw ex;
        }
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(@RequestBody String refreshToken) {
        AuthResponse resp = authService.refresh(refreshToken);
        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(@RequestBody String refreshToken) {
        authService.logout(refreshToken);
        return ResponseEntity.ok().build();
    }
}
