package com.megamart.backend.config;

import lombok.RequiredArgsConstructor;
import org.springframework.context.annotation.Configuration;
import org.springframework.web.servlet.config.annotation.InterceptorRegistry;
import org.springframework.web.servlet.config.annotation.WebMvcConfigurer;

@Configuration
@RequiredArgsConstructor
public class WebConfig implements WebMvcConfigurer {

    private final IpTrackingInterceptor ipTrackingInterceptor;

    @Override
    public void addInterceptors(InterceptorRegistry registry) {
        registry.addInterceptor(ipTrackingInterceptor)
                .addPathPatterns("/api/**") // Apply to API endpoints
                .excludePathPatterns("/api/v1/auth/**"); // Exclude auth if needed, but Login is dealt separately.
        // Actually we can include all, but maybe exclude login to avoid double
        // recording if login handles it.
        // Login calls recordLogin, which creates a NEW session.
        // recordHourlyIp updates EXISTING session.
        // If I hit login, the session is just starting. recordHourlyIp will check
        // findTopByUserId... which might be the OLD one if not committed, or the NEW
        // one.
        // To be safe, maybe exclude auth endpoints.
    }
}
