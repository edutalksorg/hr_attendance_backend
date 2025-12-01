package com.megamart.backend.auth;

import com.megamart.backend.security.JwtService;
import com.megamart.backend.user.*;
import com.megamart.backend.profile.UserProfileService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileService profileService;

    // ----------------------------------------------
    // REGISTER
    // ----------------------------------------------
    public AuthResponse register(RegisterRequest req) {

        UserRole role = UserRole.EMPLOYEE;
        if (req.getRole() != null && !req.getRole().isEmpty()) {
            try {
                role = UserRole.valueOf(req.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                // Default to EMPLOYEE if invalid role
                role = UserRole.EMPLOYEE;
            }
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .status(UserStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .locationJson(null)
                .build();

        userRepository.save(user);

        // Auto-create user profile
        profileService.createProfile(user.getId());

        // Do not generate tokens if user is pending
        if (user.getStatus() == UserStatus.PENDING) {
            return new AuthResponse(null, null);
        }

        String access = jwtService.generateToken(user);
        String refresh = createRefreshToken(user);

        return new AuthResponse(access, refresh);
    }

    // ----------------------------------------------
    // LOGIN
    // ----------------------------------------------
    public AuthResponse login(AuthRequest req, String ip) {

        User user = userRepository.findByEmail(req.getEmail())
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        if (user.getStatus() == UserStatus.PENDING) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Account is pending approval. Please wait for admin approval.");
        }

        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.FORBIDDEN,
                    "Account is blocked. Please contact administrator.");
        }

        // Do not record last login/IP for ADMIN users per requirements
        if (user.getRole() != com.megamart.backend.user.UserRole.ADMIN) {
            user.setLastLogin(OffsetDateTime.now());
            user.setLastIp(ip);
            userRepository.save(user);
        }

        String access = jwtService.generateToken(user);
        String refresh = createRefreshToken(user);

        return new AuthResponse(access, refresh);
    }

    // ----------------------------------------------
    // REFRESH TOKEN
    // ----------------------------------------------
    public AuthResponse refresh(String refreshToken) {

        RefreshToken r = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new RuntimeException("Invalid refresh token"));

        if (r.isRevoked())
            throw new RuntimeException("Token revoked");

        if (r.getExpiresAt().isBefore(OffsetDateTime.now())) {
            throw new RuntimeException("Refresh token expired");
        }

        User user = r.getUser();

        String newAccess = jwtService.generateToken(user);
        String newRefresh = createRefreshToken(user);

        return new AuthResponse(newAccess, newRefresh);
    }

    // ----------------------------------------------
    // LOGOUT
    // ----------------------------------------------
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    // ----------------------------------------------
    // CREATE REFRESH TOKEN
    // ----------------------------------------------
    private String createRefreshToken(User user) {

        String token = UUID.randomUUID().toString();

        RefreshToken r = RefreshToken.builder()
                .user(user)
                .token(token)
                .createdAt(OffsetDateTime.now())
                .expiresAt(OffsetDateTime.now().plusDays(30))
                .revoked(false)
                .build();

        refreshTokenRepository.save(r);
        return token;
    }
}
