package com.megamart.backend.auth;

import com.megamart.backend.profile.UserProfileService;
import com.megamart.backend.user.ApprovalRepository;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;
import com.megamart.backend.user.UserRole;
import com.megamart.backend.user.UserStatus;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.server.ResponseStatusException;

import java.util.UUID;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.Mockito.*;

public class AuthServiceTest {

    private UserRepository userRepository;
    private ApprovalRepository approvalRepository;
    private RefreshTokenRepository refreshTokenRepository;
    private PasswordEncoder passwordEncoder;
    private Object jwtService; // avoid loading JwtService implementation at test-class-load time
    private UserProfileService profileService;

    private AuthService authService;

    @BeforeEach
    void setUp() {
        userRepository = mock(UserRepository.class);
        approvalRepository = mock(ApprovalRepository.class);
        refreshTokenRepository = mock(RefreshTokenRepository.class);
        passwordEncoder = mock(PasswordEncoder.class);
        jwtService = null; // not required for register() tests
        profileService = mock(UserProfileService.class);

        authService = new AuthService(userRepository, approvalRepository, refreshTokenRepository, passwordEncoder, null, profileService);
    }

    @Test
    void register_with_valid_roles_saved_as_requested() {
        when(userRepository.existsByEmail("a@b.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L); // not first user
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("UA");
        req.setEmail("a@b.com");
        req.setPassword("pass");
        req.setRole("ADMIN");

        authService.register(req);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());

        User u = saved.getValue();
        assertEquals(UserRole.ADMIN, u.getRole());
        assertEquals(UserStatus.PENDING, u.getStatus());

        // test other role
        req.setRole("marketing");
        authService.register(req);
        verify(userRepository, times(2)).save(saved.capture());
        assertEquals(UserRole.MARKETING, saved.getValue().getRole());
    }

    @Test
    void register_with_invalid_role_throws_400() {
        when(userRepository.existsByEmail("a@b2.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("pass")).thenReturn("encoded");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("UA");
        req.setEmail("a@b2.com");
        req.setPassword("pass");
        req.setRole("invalid-role");

        assertThrows(ResponseStatusException.class, () -> authService.register(req));
    }

    @Test
    void register_with_no_role_defaults_to_employee() {
        when(userRepository.existsByEmail("x@x.com")).thenReturn(false);
        when(userRepository.count()).thenReturn(1L);
        when(passwordEncoder.encode("p")).thenReturn("encoded");

        RegisterRequest req = new RegisterRequest();
        req.setFullName("UA");
        req.setEmail("x@x.com");
        req.setPassword("p");

        authService.register(req);

        ArgumentCaptor<User> saved = ArgumentCaptor.forClass(User.class);
        verify(userRepository).save(saved.capture());
        assertEquals(UserRole.EMPLOYEE, saved.getValue().getRole());
    }
}
