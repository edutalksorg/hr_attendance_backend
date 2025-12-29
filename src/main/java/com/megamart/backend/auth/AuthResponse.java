package com.megamart.backend.auth;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.AllArgsConstructor;
import lombok.Data;

@Data
@AllArgsConstructor
public class AuthResponse {
    private String accessToken;

    @JsonIgnore
    private String refreshToken;

    private com.megamart.backend.user.User user;
}
