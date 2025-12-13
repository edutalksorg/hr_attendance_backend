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
@SuppressWarnings("null")
public class AuthService {

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileService profileService;
    private final ApprovalRepository approvalRepository;

    // Admin Secret Code - Change this in production!

    // ----------------------------------------------
    // REGISTER
    // ----------------------------------------------
    public AuthResponse register(RegisterRequest req) {

        // Check if email already exists
        if (userRepository.existsByEmail(req.getEmail())) {
            throw new RuntimeException("Email already registered");
        }

        // Determine role and status based on admin secret code
        UserRole role = UserRole.EMPLOYEE;
        UserStatus status = UserStatus.PENDING;

        // Check if a specific role was requested
        if (req.getRole() != null && !req.getRole().isEmpty()) {
            try {
                UserRole requestedRole = UserRole.valueOf(req.getRole());
                role = requestedRole;
            } catch (IllegalArgumentException e) {
                // specific role not found, defaulting to EMPLOYEE
            }
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .locationJson(null)
                .build();

        userRepository.save(user);

        // Create approval request if PENDING
        if (status == UserStatus.PENDING) {
            Approval ap = Approval.builder()
                    .targetUserId(user.getId())
                    .approvalType("REGISTRATION")
                    .roleAfter(role) // Save the requested role so Admin knows what to approve
                    .status("PENDING")
                    .createdAt(OffsetDateTime.now())
                    .build();
            approvalRepository.save(ap);
        }

        // Auto-create user profile
        profileService.createProfile(user.getId());

        // Only return tokens if user is ACTIVE (admins)
        // PENDING users do NOT get tokens, ensuring they cannot access the app
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
                .orElseThrow(() -> new RuntimeException("Invalid credentials"));

        if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid credentials");
        }

        // ⚠️ CRITICAL: Check user status in the correct order
        // 1. Check if user is blocked FIRST
        if (user.getStatus() == UserStatus.BLOCKED) {
            throw new RuntimeException("You are blocked by admin");
        }

        // 2. Check if user approval is pending
        if (user.getStatus() == UserStatus.PENDING) {
            throw new RuntimeException("Your approval is not completed. Please contact Admin/HR.");
        }

        // 3. Only ACTIVE users can login
        if (user.getStatus() != UserStatus.ACTIVE) {
            throw new RuntimeException("Account not active. Please contact admin.");
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
    // CHANGE PASSWORD
    // ----------------------------------------------
    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new RuntimeException("User not found"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new RuntimeException("Invalid current password");
        }

        user.setPassword(passwordEncoder.encode(req.getNewPassword()));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);
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
