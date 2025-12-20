package com.megamart.backend.config;

import io.swagger.v3.oas.annotations.OpenAPIDefinition;
import io.swagger.v3.oas.annotations.enums.SecuritySchemeType;
import io.swagger.v3.oas.annotations.info.Info;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.security.SecurityScheme;
import org.springframework.context.annotation.Configuration;

@Configuration
@OpenAPIDefinition(info = @Info(title = "MegaMart API", version = "v1", description = "API Documentation for MegaMart HR Attendance System"), security = @SecurityRequirement(name = "BearerAuth"), servers = {
        @io.swagger.v3.oas.annotations.servers.Server(url = "http://localhost:9090", description = "Local Environment"),
        @io.swagger.v3.oas.annotations.servers.Server(url = "https://hrbackend.edu-attendance.work.gd", description = "Production Environment")
})
@SecurityScheme(name = "BearerAuth", type = SecuritySchemeType.HTTP, scheme = "bearer", bearerFormat = "JWT")
public class OpenApiConfig {
}
