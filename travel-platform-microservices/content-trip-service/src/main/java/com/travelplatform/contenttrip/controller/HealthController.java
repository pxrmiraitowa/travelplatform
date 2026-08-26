package com.travelplatform.contenttrip.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.HealthStatusVO;
import com.travelplatform.common.vo.VersionVO;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    private static final DateTimeFormatter FORMATTER = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
    private final String startedAt = LocalDateTime.now().format(FORMATTER);

    @Value("${spring.application.name:content-trip-service}")
    private String serviceName;

    @Value("${service.version:0.0.1-SNAPSHOT}")
    private String version;

    @GetMapping("/health")
    public Result<HealthStatusVO> health() {
        return Result.success(new HealthStatusVO(serviceName, "UP", serviceName + " is running", now()));
    }

    @GetMapping("/version")
    public Result<VersionVO> version() {
        return Result.success(new VersionVO(serviceName, version, startedAt));
    }

    private String now() {
        return LocalDateTime.now().format(FORMATTER);
    }
}
