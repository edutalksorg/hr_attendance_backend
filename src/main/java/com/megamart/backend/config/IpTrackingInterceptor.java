package com.megamart.backend.config;

import com.megamart.backend.attendance.AttendanceService;
import com.megamart.backend.security.IpDetectionService;
import com.megamart.backend.user.User;
import com.megamart.backend.user.UserRole;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

@Component
@RequiredArgsConstructor
public class IpTrackingInterceptor implements HandlerInterceptor {

    private final AttendanceService attendanceService;
    private final IpDetectionService ipService;

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        try {
            Authentication auth = SecurityContextHolder.getContext().getAuthentication();
            if (auth != null && auth.isAuthenticated()) {
                Object principal = auth.getPrincipal();
                if (principal instanceof com.megamart.backend.security.CustomUserDetails) {
                    User user = ((com.megamart.backend.security.CustomUserDetails) principal).getUser();
                    if (user.getRole() == UserRole.MARKETING_EXECUTIVE) {
                        String ip = ipService.getClientIp(request);
                        attendanceService.recordHourlyIp(user.getId(), ip);
                    }
                }
            }
        } catch (Exception e) {
            // ignore to not block request
        }
        return true;
    }
}
