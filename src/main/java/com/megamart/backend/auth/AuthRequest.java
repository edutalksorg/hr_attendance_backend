package com.megamart.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class AuthRequest {
    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    @NotBlank(message = "password is required")
    private String password;
}
