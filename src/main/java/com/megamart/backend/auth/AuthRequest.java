package com.megamart.backend.auth;

import lombok.Data;

@Data
public class AuthRequest {
    private String email;
    private String password;
    private String ipAddress;
    private Double latitude;
    private Double longitude;
}
