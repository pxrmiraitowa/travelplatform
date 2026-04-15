package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.admin.auth.AdminLoginRequest;
import com.travelplatform.dto.auth.LoginResponse;
import com.travelplatform.service.admin.AdminAuthService;
import com.travelplatform.vo.user.CurrentUserVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/auth")
public class AdminAuthController {

    private final AdminAuthService adminAuthService;

    public AdminAuthController(AdminAuthService adminAuthService) {
        this.adminAuthService = adminAuthService;
    }

    @Operation(summary = "管理员登录")
    @PostMapping("/login")
    public Result<LoginResponse> login(@Valid @RequestBody AdminLoginRequest request) {
        return Result.success(adminAuthService.login(request));
    }

    @Operation(summary = "获取当前管理员信息")
    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentAdmin() {
        return Result.success(adminAuthService.getCurrentAdmin());
    }
}
