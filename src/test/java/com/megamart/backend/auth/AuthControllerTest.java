package com.megamart.backend.auth;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.mockito.Mockito.doNothing;
import static org.mockito.Mockito.doThrow;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;

@WebMvcTest(controllers = AuthController.class)
@AutoConfigureMockMvc(addFilters = false)
public class AuthControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private AuthService authService;

    @MockBean
    private com.megamart.backend.security.JwtService jwtService;

    @MockBean
    private com.megamart.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @MockBean
    private com.megamart.backend.security.CustomUserDetailsService customUserDetailsService;

    @Test
    void register_returns_success_message() throws Exception {
        String payload = "{\"fullName\":\"Jane Doe\",\"email\":\"jane@example.com\",\"password\":\"pass1234\"}";

        doNothing().when(authService).register(org.mockito.Mockito.any());

        mvc.perform(post("/api/auth/register")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"User registered successfully — waiting for approval\"}"));
    }

    @Test
    void login_pending_returns_401_and_message() throws Exception {
        String payload = "{\"email\":\"jane@example.com\",\"password\":\"pass1234\"}";

        doThrow(new org.springframework.web.server.ResponseStatusException(
                org.springframework.http.HttpStatus.UNAUTHORIZED,
                "Account pending approval"))
                .when(authService).login(org.mockito.Mockito.any(), org.mockito.Mockito.anyString());

        mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(payload))
                .andExpect(status().isUnauthorized())
                .andExpect(content().json("{\"message\":\"Account pending approval\"}"));
    }
}
