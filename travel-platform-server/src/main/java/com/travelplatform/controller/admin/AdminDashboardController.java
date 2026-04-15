package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.service.admin.AdminDashboardService;
import com.travelplatform.vo.admin.AdminDashboardVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/dashboard")
public class AdminDashboardController {

    private final AdminDashboardService adminDashboardService;

    public AdminDashboardController(AdminDashboardService adminDashboardService) {
        this.adminDashboardService = adminDashboardService;
    }

    @Operation(summary = "后台控制台概览")
    @GetMapping
    public Result<AdminDashboardVO> getDashboard() {
        return Result.success(adminDashboardService.getDashboard());
    }
}
