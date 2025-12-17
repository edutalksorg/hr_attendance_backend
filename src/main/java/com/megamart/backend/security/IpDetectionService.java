package com.megamart.backend.security;

import jakarta.servlet.http.HttpServletRequest;
import lombok.RequiredArgsConstructor;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestTemplate;

import java.util.Map;

@Service
@RequiredArgsConstructor
public class IpDetectionService {

    private static final Logger logger = LoggerFactory.getLogger(IpDetectionService.class);
    private final RestTemplate restTemplate;

    public String getClientIp(HttpServletRequest request) {
        String ip = request.getHeader("X-Forwarded-For");
        if (ip == null || ip.isEmpty() || "unknown".equalsIgnoreCase(ip)) {
            ip = request.getRemoteAddr();
        } else {
            // X-Forwarded-For might be a list
            if (ip.contains(",")) {
                ip = ip.split(",")[0].trim();
            }
        }

        // Check for localhost
        if (ip == null || ip.equals("127.0.0.1") || ip.equals("0:0:0:0:0:0:0:1")) {
            try {
                logger.info("Local IP detected ({}), fetching public IP from ipify...", ip);
                // "https://api.ipify.org?format=json" returns {"ip":"..."}
                Map<String, String> response = restTemplate.getForObject("https://api.ipify.org?format=json",
                        Map.class);
                if (response != null && response.containsKey("ip")) {
                    ip = response.get("ip");
                    logger.info("Public IP fetched: {}", ip);
                }
            } catch (Exception e) {
                logger.error("Failed to fetch public IP from ipify: {}", e.getMessage());
                // Fallback to local IP if API fails
            }
        }
        return ip;
    }
}
