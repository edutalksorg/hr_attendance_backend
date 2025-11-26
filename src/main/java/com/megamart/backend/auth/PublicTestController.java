package com.megamart.backend.auth;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class PublicTestController {

    @GetMapping("/ping")
    public String ping() {
        return "OK";
    }

}
