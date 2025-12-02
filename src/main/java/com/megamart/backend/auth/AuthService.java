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
    private final com.megamart.backend.user.ApprovalRepository approvalRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileService profileService;

    // ----------------------------------------------
    // REGISTER
    // ----------------------------------------------
    public void register(RegisterRequest req) {

        // Validate role: if empty -> default EMPLOYEE. If provided and invalid -> BAD_REQUEST
        UserRole role;
        if (req.getRole() == null || req.getRole().isBlank()) {
            role = UserRole.EMPLOYEE;
        } else {
            try {
                role = UserRole.valueOf(req.getRole().toUpperCase());
            } catch (IllegalArgumentException e) {
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST,
                        "Invalid role"
                );
            }
        }

        if (userRepository.existsByEmail(req.getEmail())) {
            throw new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.CONFLICT,
                "Email already exists"
            );
        }

        boolean isFirstUser = userRepository.count() == 0;

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
            // if this is the very first user in empty DB, grant ADMIN and ACTIVE
            .role(isFirstUser ? UserRole.ADMIN : role)
            .status(isFirstUser ? UserStatus.ACTIVE : UserStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .locationJson(null)
                .build();

        userRepository.save(user);

        // Auto-create user profile
        profileService.createProfile(user.getId());

        // Only create an approval entry for non-admin users (first user or admin do not require approval)
        if (!isFirstUser && user.getStatus() == UserStatus.PENDING) {
            com.megamart.backend.user.Approval ap = com.megamart.backend.user.Approval.builder()
                    .targetUserId(user.getId())
                    .approvalType("REGISTRATION")
                    .status("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .build();
            approvalRepository.save(ap);
        }

        // Do not generate tokens when registering. Controller will return a message.
        return;
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
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Account pending approval");
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
