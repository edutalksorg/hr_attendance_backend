package com.megamart.backend.auth;

import lombok.Data;

@Data
public class RegisterRequest {
    private String fullName;
    private String email;
    private String phone;
    private String password;
    private String adminCode; // Optional: Use "ADMIN@MEGAMART2025" to register as admin
    private String role; // Optional: EMPLOYEE, MARKETING_EXECUTIVE, HR (if allowed)
    private java.util.UUID branchId;
}
