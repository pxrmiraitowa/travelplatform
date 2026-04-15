package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.vo.HealthStatusVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/public")
public class HealthController {

    @Operation(summary = "健康检查")
    @GetMapping("/health")
    public Result<HealthStatusVO> health() {
        return Result.success(new HealthStatusVO("UP", "travel-platform-server is running"));
    }
}
