package com.megamart.backend.auth;

import com.megamart.backend.security.JwtService;
import com.megamart.backend.user.*;
import com.megamart.backend.profile.UserProfileService;
import com.megamart.backend.email.EmailService;
import jakarta.transaction.Transactional;
import lombok.RequiredArgsConstructor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class AuthService {

    private static final Logger logger = LoggerFactory.getLogger(AuthService.class);

    private final UserRepository userRepository;
    private final RefreshTokenRepository refreshTokenRepository;
    private final PasswordEncoder passwordEncoder;
    private final JwtService jwtService;
    private final UserProfileService profileService;
    private final ApprovalRepository approvalRepository;
    private final PasswordResetTokenRepository passwordResetTokenRepository;
    private final EmailService emailService;

    // Admin Secret Code - Change this in production!

    // ----------------------------------------------
    // REGISTER
    // ----------------------------------------------
    @Transactional
    public AuthResponse register(RegisterRequest req) {
        logger.info("📝 Registration attempt for email: {}, role: {}", req.getEmail(), req.getRole());

        // Check if email already exists
        if (userRepository.existsByEmail(req.getEmail())) {
            logger.warn("❌ Registration failed: Email {} already exists", req.getEmail());
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.CONFLICT, "Email already registered");
        }

        // Determine role and status
        UserRole role = UserRole.EMPLOYEE;
        UserStatus status = UserStatus.PENDING;

        // Check if a specific role was requested
        if (req.getRole() != null && !req.getRole().isEmpty()) {
            try {
                String normalizedRole = req.getRole().trim().toUpperCase().replace(" ", "_");
                if ("MARKETING".equals(normalizedRole)) {
                    normalizedRole = "MARKETING_EXECUTIVE";
                }
                UserRole requestedRole = UserRole.valueOf(normalizedRole);
                role = requestedRole;
                logger.info("✅ Requested role '{}' mapped to {}", req.getRole(), role);
            } catch (IllegalArgumentException e) {
                logger.warn("⚠️ Requested role '{}' not found, defaulting to EMPLOYEE", req.getRole());
            }
        }

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .phone(req.getPhone())
                .role(role)
                .status(status)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();

        try {
            user = userRepository.save(user);
            logger.info("✅ User saved with ID: {}", user.getId());

            // Create approval request if PENDING
            if (status == UserStatus.PENDING) {
                Approval ap = Approval.builder()
                        .targetUserId(user.getId())
                        .approvalType("REGISTRATION")
                        .roleAfter(role)
                        .status("PENDING")
                        .createdAt(OffsetDateTime.now())
                        .build();
                approvalRepository.save(ap);
                logger.info("✅ Approval request created for user: {}", user.getId());
            }

            // Auto-create user profile
            profileService.createProfile(user.getId());
            logger.info("✅ User profile created for user: {}", user.getId());

            // Only return tokens if user is ACTIVE (admins)
            if (user.getStatus() == UserStatus.PENDING) {
                logger.info("⌛ Registration complete. User {} is pending approval.", user.getEmail());
                return new AuthResponse(null, null, null);
            }

            String access = jwtService.generateToken(user);
            String refresh = createRefreshToken(user);

            logger.info("🎉 Registration successful for ACTIVE user: {}", user.getEmail());
            return new AuthResponse(access, refresh, user);
        } catch (Exception e) {
            logger.error("💥 Registration failed for {}: {}", req.getEmail(), e.getMessage(), e);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                    "Registration failed: " + e.getMessage());
        }
    }

    // ----------------------------------------------
    // LOGIN
    // ----------------------------------------------
    public AuthResponse login(AuthRequest req, String ip) {

        logger.info("🔐 Login attempt for email: {}", req.getEmail());

        try {
            User user = userRepository.findByEmail(req.getEmail())
                    .orElseThrow(() -> {
                        logger.warn("❌ User not found: {}", req.getEmail());
                        return new org.springframework.web.server.ResponseStatusException(
                                org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
                    });

            logger.info("✅ User found: {} (Role: {}, Status: {})",
                    user.getEmail(), user.getRole(), user.getStatus());

            if (user.getPassword() == null) {
                logger.error("❌ User has null password: {}", req.getEmail());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Account data corrupted (missing password). Please contact admin.");
            }

            if (!passwordEncoder.matches(req.getPassword(), user.getPassword())) {
                logger.warn("❌ Password mismatch for user: {}", req.getEmail());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid credentials");
            }

            logger.info("✅ Password validated for user: {}", req.getEmail());

            // ⚠️ CRITICAL: Check user status in the correct order
            // Handle null status gracefully (treat as PENDING or invalid)
            if (user.getStatus() == null) {
                logger.error("❌ User has null status: {}", req.getEmail());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.INTERNAL_SERVER_ERROR,
                        "Account status unknown. Please contact admin.");
            }

            // 1. Check if user is blocked FIRST
            if (user.getStatus() == UserStatus.BLOCKED) {
                logger.warn("❌ Blocked user attempted login: {}", req.getEmail());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Contact admin you are blocked");
            }

            // 2. Check if user approval is pending
            if (user.getStatus() == UserStatus.PENDING) {
                logger.warn("❌ Pending user attempted login: {}", req.getEmail());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN,
                        "Your approval is not completed. Please contact Admin/HR.");
            }

            // 3. Only ACTIVE users can login
            if (user.getStatus() != UserStatus.ACTIVE) {
                logger.warn("❌ Inactive user attempted login: {} (Status: {})",
                        req.getEmail(), user.getStatus());
                throw new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.FORBIDDEN, "Account not active. Please contact admin.");
            }

            // Record last login/IP for ALL users roles
            user.setLastLogin(OffsetDateTime.now());
            user.setLastIp(ip);
            userRepository.save(user);

            logger.info("✅ Generating tokens for user: {}", req.getEmail());

            String access = jwtService.generateToken(user);
            String refresh = createRefreshToken(user);

            logger.info("🎉 Login successful for user: {}", req.getEmail());

            return new AuthResponse(access, refresh, user);
        } catch (Exception e) {
            logger.error("💥 Login failed for {}: {}", req.getEmail(), e.getMessage(), e);
            throw e;
        }
    }

    // ----------------------------------------------
    // REFRESH TOKEN
    // ----------------------------------------------
    @Transactional
    public AuthResponse refresh(String refreshToken) {

        RefreshToken r = refreshTokenRepository.findByToken(refreshToken)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid refresh token"));

        if (r.isRevoked())
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Token revoked");

        if (r.getExpiresAt().isBefore(OffsetDateTime.now())) {
            refreshTokenRepository.delete(r);
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Refresh token expired");
        }

        User user = r.getUser();

        // Rotation: Delete the old token
        refreshTokenRepository.delete(r);

        String newAccess = jwtService.generateToken(user);
        String newRefresh = createRefreshToken(user);

        return new AuthResponse(newAccess, newRefresh, user);
    }

    // ----------------------------------------------
    // LOGOUT
    // ----------------------------------------------
    @Transactional
    public void logout(String refreshToken) {
        refreshTokenRepository.deleteByToken(refreshToken);
    }

    // ----------------------------------------------
    // FORGOT PASSWORD
    // ----------------------------------------------
    public void forgotPassword(String email) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found with email: " + email));

        // Generate token
        String token = UUID.randomUUID().toString();

        // Save token
        // Remove existing token if any
        passwordResetTokenRepository.findByUser(user).ifPresent(passwordResetTokenRepository::delete);

        PasswordResetToken resetToken = PasswordResetToken.builder()
                .token(token)
                .user(user)
                .expiryDate(java.time.LocalDateTime.now().plusHours(24))
                .build();
        passwordResetTokenRepository.save(resetToken);

        // Send Email
        String resetLink = "http://localhost:5173/reset-password?token=" + token;
        String subject = "Password Reset Request";
        String body = "<div style=\"background-color: #f3f4f6; padding: 20px; font-family: Arial, sans-serif;\">\n" +
                "  <div style=\"max-width: 400px; margin: 0 auto; background-color: white; padding: 40px; border-radius: 8px; box-shadow: 0 4px 6px rgba(0,0,0,0.1); text-align: center;\">\n"
                +
                "    <h1 style=\"margin-bottom: 24px; font-size: 32px;\">\n" +
                "      <span style=\"color: #ef4444; font-weight: bold;\">Edu</span><span style=\"color: black; font-weight: bold;\">Talks</span>\n"
                +
                "    </h1>\n" +
                "    <h2 style=\"color: #111827; margin-bottom: 16px; font-size: 24px; font-weight: bold;\">Password Reset</h2>\n"
                +
                "    <p style=\"color: #6b7280; margin-bottom: 32px; font-size: 16px;\">To reset your password, click the button below:</p>\n"
                +
                "    <a href=\"" + resetLink
                + "\" style=\"display: inline-block; background-color: #3b82f6; color: white; padding: 12px 24px; font-size: 16px; font-weight: bold; text-decoration: none; border-radius: 6px;\">Reset Password</a>\n"
                +
                "    <p style=\"margin-top: 32px; color: #9ca3af; font-size: 14px;\">If you didn't request a password reset, you can safely ignore this email.</p>\n"
                +
                "  </div>\n" +
                "</div>";

        emailService.sendEmail(user.getEmail(), subject, body);
    }

    public void resetPassword(String token, String newPassword) {
        PasswordResetToken resetToken = passwordResetTokenRepository.findByToken(token)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.BAD_REQUEST, "Invalid token"));

        if (resetToken.isExpired()) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.BAD_REQUEST, "Token expired");
        }

        User user = resetToken.getUser();
        user.setPassword(passwordEncoder.encode(newPassword));
        user.setUpdatedAt(OffsetDateTime.now());
        userRepository.save(user);

        // Consume token
        passwordResetTokenRepository.delete(resetToken);
    }

    // ----------------------------------------------
    // CHANGE PASSWORD
    // ----------------------------------------------
    @Transactional
    public void changePassword(String email, ChangePasswordRequest req) {
        User user = userRepository.findByEmail(email)
                .orElseThrow(() -> new org.springframework.web.server.ResponseStatusException(
                        org.springframework.http.HttpStatus.NOT_FOUND, "User not found"));

        if (!passwordEncoder.matches(req.getOldPassword(), user.getPassword())) {
            throw new org.springframework.web.server.ResponseStatusException(
                    org.springframework.http.HttpStatus.UNAUTHORIZED, "Invalid current password");
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
