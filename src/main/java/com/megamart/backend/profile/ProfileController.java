package com.megamart.backend.profile;

import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.*;

import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRepository;

import java.util.UUID;

@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
public class ProfileController {
    private final UserProfileService profileService;
    private final UserRepository userRepository;

    public static record UpdateProfileRequest(@NotBlank String username, String bio) {}
    public static record PhotoUploadRequest(@NotBlank String photoUrl) {}

    @GetMapping("/me")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<UserProfileDto> getMyProfile() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfileEntity profile = profileService.getProfile(user.getId());
        return ResponseEntity.ok(toDto(profile));
    }

    @PutMapping("/update")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<UserProfileDto> updateProfile(@RequestBody UpdateProfileRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfileEntity profile = profileService.updateProfile(user.getId(), req.username(), req.bio());
        return ResponseEntity.ok(toDto(profile));
    }

    @PostMapping("/photo")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<UserProfileDto> uploadPhoto(@RequestBody PhotoUploadRequest req) {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        User user = userRepository.findByEmail(email).orElseThrow(() -> new RuntimeException("User not found"));
        UserProfileEntity profile = profileService.uploadPhoto(user.getId(), req.photoUrl());
        return ResponseEntity.ok(toDto(profile));
    }

    @GetMapping("/{userId}")
    @PreAuthorize("hasAnyRole('ADMIN','HR','EMPLOYEE')")
    public ResponseEntity<UserProfileDto> getPublicProfile(@PathVariable UUID userId) {
        UserProfileEntity profile = profileService.getProfile(userId);
        return ResponseEntity.ok(toDto(profile));
    }

    private UserProfileDto toDto(UserProfileEntity entity) {
        return UserProfileDto.builder()
                .id(entity.getId())
                .userId(entity.getUserId())
                .username(entity.getUsername())
                .bio(entity.getBio())
                .photoUrl(entity.getPhotoUrl())
                .createdAt(entity.getCreatedAt())
                .updatedAt(entity.getUpdatedAt())
                .build();
    }
}
