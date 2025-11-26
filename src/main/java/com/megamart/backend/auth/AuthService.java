package com.megamart.backend.auth;

import com.megamart.backend.security.JwtService;
import com.megamart.backend.user.*;
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

    // ----------------------------------------------
    // REGISTER
    // ----------------------------------------------
    public AuthResponse register(RegisterRequest req) {

        User user = User.builder()
                .fullName(req.getFullName())
                .email(req.getEmail())
                .password(passwordEncoder.encode(req.getPassword()))
                .role(UserRole.EMPLOYEE)
                .status(UserStatus.PENDING)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .locationJson(null)
                .build();

        userRepository.save(user);

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

        user.setLastLogin(OffsetDateTime.now());
        user.setLastIp(ip);
        userRepository.save(user);

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

        if (r.isRevoked()) throw new RuntimeException("Token revoked");

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
