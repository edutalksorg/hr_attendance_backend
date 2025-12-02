package com.megamart.backend.user;

import com.megamart.backend.auth.ApiResponse;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.test.web.servlet.MockMvc;

import java.util.UUID;

import static org.mockito.Mockito.doReturn;
import static org.mockito.Mockito.doNothing;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@WebMvcTest(controllers = ApprovalController.class)
@org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc(addFilters = false)
public class ApprovalControllerTest {

    @Autowired
    private MockMvc mvc;

    @MockBean
    private UserRepository userRepository;

    @MockBean
    private UserService userService;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.megamart.backend.security.JwtService jwtService;

        @org.springframework.boot.test.mock.mockito.MockBean
        private com.megamart.backend.security.JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void hr_cannot_approve_admin() throws Exception {
        String approverEmail = "hr@megamart.com";
        // set security context
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(approverEmail, null)
        );

        User approver = User.builder().id(UUID.randomUUID()).email(approverEmail).role(UserRole.HR).build();
        doReturn(java.util.Optional.of(approver)).when(userRepository).findByEmail(approverEmail);

        // HR cannot approve ADMIN
        mvc.perform(post("/api/users/approve/" + UUID.randomUUID()).param("role", "ADMIN")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isForbidden())
                .andExpect(content().json("{\"message\":\"You are not authorized to approve this role\"}"));
    }

    @Test
    void hr_can_approve_employee() throws Exception {
        String approverEmail = "hr@megamart.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(approverEmail, null)
        );

        UUID targetId = UUID.randomUUID();
        User approver = User.builder().id(UUID.randomUUID()).email(approverEmail).role(UserRole.HR).build();
        doReturn(java.util.Optional.of(approver)).when(userRepository).findByEmail(approverEmail);

        // HR can approve EMPLOYEE
        doReturn(true).when(userService).canApprove(UserRole.HR, UserRole.EMPLOYEE);
        doReturn(User.builder().id(targetId).status(UserStatus.ACTIVE).role(UserRole.EMPLOYEE).build())
                .when(userService).approveUser(approver.getId(), targetId, UserRole.EMPLOYEE);

        mvc.perform(post("/api/users/approve/" + targetId).param("role", "EMPLOYEE")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"User approved successfully\"}"));
    }

    @Test
    void admin_can_approve_hr() throws Exception {
        String approverEmail = "admin@megamart.com";
        SecurityContextHolder.getContext().setAuthentication(
                new UsernamePasswordAuthenticationToken(approverEmail, null)
        );

        UUID targetId = UUID.randomUUID();
        User approver = User.builder().id(UUID.randomUUID()).email(approverEmail).role(UserRole.ADMIN).build();
        doReturn(java.util.Optional.of(approver)).when(userRepository).findByEmail(approverEmail);

        doReturn(true).when(userService).canApprove(UserRole.ADMIN, UserRole.HR);
        doReturn(User.builder().id(targetId).status(UserStatus.ACTIVE).role(UserRole.HR).build())
                .when(userService).approveUser(approver.getId(), targetId, UserRole.HR);

        mvc.perform(post("/api/users/approve/" + targetId).param("role", "HR")
                        .contentType(MediaType.APPLICATION_JSON))
                .andExpect(status().isOk())
                .andExpect(content().json("{\"message\":\"User approved successfully\"}"));
    }
}
