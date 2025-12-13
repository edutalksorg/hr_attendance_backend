package com.megamart.backend.profile;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;
import java.util.UUID;

@Service
@RequiredArgsConstructor
@SuppressWarnings("null")
public class UserProfileService {
    private final UserProfileRepository repository;

    public UserProfileEntity createProfile(UUID userId) {
        UserProfileEntity p = UserProfileEntity.builder()
                .userId(userId)
                .username(null)
                .bio(null)
                .photoUrl(null)
                .createdAt(OffsetDateTime.now())
                .updatedAt(OffsetDateTime.now())
                .build();
        return repository.save(p);
    }

    public UserProfileEntity getProfile(UUID userId) {
        return repository.findByUserId(userId)
                .orElseThrow(() -> new RuntimeException("Profile not found for user"));
    }

    public UserProfileEntity updateProfile(UUID userId, String username, String bio) {
        UserProfileEntity p = getProfile(userId);
        if (username != null)
            p.setUsername(username);
        if (bio != null)
            p.setBio(bio);
        p.setUpdatedAt(OffsetDateTime.now());
        return repository.save(p);
    }

    public UserProfileEntity uploadPhoto(UUID userId, String photoUrl) {
        UserProfileEntity p = getProfile(userId);
        p.setPhotoUrl(photoUrl);
        p.setUpdatedAt(OffsetDateTime.now());
        return repository.save(p);
    }
}
