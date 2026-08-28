package com.travelplatform.gateway.controller;

import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {
    private final String startedAt = LocalDateTime.now().toString();

    @Value("${spring.application.name:gateway-service}")
    private String serviceName;

    @Value("${service.version:0.0.1-SNAPSHOT}")
    private String version;

    @GetMapping("/health")
    public Map<String, Object> health() {
        return Map.of(
                "code", 200,
                "message", "操作成功",
                "data", Map.of(
                        "serviceName", serviceName,
                        "status", "UP",
                        "message", serviceName + " is running",
                        "checkedAt", LocalDateTime.now().toString()
                )
        );
    }

    @GetMapping("/version")
    public Map<String, Object> version() {
        return Map.of("code", 200, "message", "操作成功", "data",
                Map.of("serviceName", serviceName, "version", version, "startedAt", startedAt));
    }
}
