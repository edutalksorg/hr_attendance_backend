package com.megamart.backend.auth;

import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.security.core.context.SecurityContextHolder;
import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
public class AuthController {
    private final AuthService authService;

    private final com.megamart.backend.security.IpDetectionService ipService;

    @PostMapping("/register")
    public ResponseEntity<AuthResponse> register(@RequestBody RegisterRequest req, HttpServletResponse response) {
        AuthResponse resp = authService.register(req);

        if (resp.getRefreshToken() != null) {
            setRefreshTokenCookie(response, resp.getRefreshToken());
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/login")
    public ResponseEntity<AuthResponse> login(@RequestBody AuthRequest req, HttpServletRequest request,
            HttpServletResponse response) {
        String ip = ipService.getClientIp(request);
        AuthResponse resp = authService.login(req, ip);

        if (resp.getRefreshToken() != null) {
            setRefreshTokenCookie(response, resp.getRefreshToken());
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/refresh")
    public ResponseEntity<AuthResponse> refresh(
            @RequestBody(required = false) TokenRequest req,
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            HttpServletResponse response) {

        String token = (req != null && req.getRefreshToken() != null) ? req.getRefreshToken() : cookieToken;

        if (token == null || token.isEmpty()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token is missing");
        }

        AuthResponse resp = authService.refresh(token);

        if (resp.getRefreshToken() != null) {
            setRefreshTokenCookie(response, resp.getRefreshToken());
        }

        return ResponseEntity.ok(resp);
    }

    @PostMapping("/logout")
    public ResponseEntity<Void> logout(
            @RequestBody(required = false) TokenRequest req,
            @CookieValue(name = "refreshToken", required = false) String cookieToken,
            HttpServletResponse response) {

        String token = (req != null && req.getRefreshToken() != null) ? req.getRefreshToken() : cookieToken;

        if (token != null && !token.isEmpty()) {
            authService.logout(token);
        }

        clearRefreshTokenCookie(response);
        return ResponseEntity.ok().build();
    }

    private void setRefreshTokenCookie(HttpServletResponse response, String token) {
        Cookie cookie = new Cookie("refreshToken", token);
        cookie.setHttpOnly(true);
        cookie.setSecure(false); // Set to true if using HTTPS (production)
        cookie.setPath("/");
        cookie.setMaxAge(30 * 24 * 60 * 60); // 30 days
        response.addCookie(cookie);
    }

    private void clearRefreshTokenCookie(HttpServletResponse response) {
        Cookie cookie = new Cookie("refreshToken", null);
        cookie.setHttpOnly(true);
        cookie.setSecure(false);
        cookie.setPath("/");
        cookie.setMaxAge(0);
        response.addCookie(cookie);
    }

    @PostMapping("/change-password")
    public ResponseEntity<Void> changePassword(@RequestBody ChangePasswordRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        authService.changePassword(email, req);
        return ResponseEntity.ok().build();
    }

    @PostMapping("/forgot-password")
    public ResponseEntity<Void> forgotPassword(@RequestBody ForgotPasswordRequest req) {
        authService.forgotPassword(req.getEmail());
        return ResponseEntity.ok().build();
    }

    @PostMapping("/reset-password")
    public ResponseEntity<Void> resetPassword(@RequestBody ResetPasswordRequest req) {
        authService.resetPassword(req.getToken(), req.getNewPassword());
        return ResponseEntity.ok().build();
    }
}
