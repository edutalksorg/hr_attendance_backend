package com.megamart.backend.auth;

import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import lombok.Data;

@Data
public class RegisterRequest {
    @NotBlank(message = "fullName is required")
    private String fullName;

    @NotBlank(message = "email is required")
    @Email(message = "email must be a valid email address")
    private String email;

    private String phone;

    @NotBlank(message = "password is required")
    private String password;

    private String role;
}
