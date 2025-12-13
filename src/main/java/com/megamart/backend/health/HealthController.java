package com.megamart.backend.health;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class HealthController {

    @GetMapping("/")
    public String home() {
        return "Hey Ram Your backend is successfully running";
    }

    @GetMapping("/health/ping")
    public String ping() {
        return "OK";
    }
}
