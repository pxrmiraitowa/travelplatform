package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.user.dto.admin.AdminUserRolesRequest;
import com.travelplatform.user.dto.admin.AdminUserStatusRequest;
import com.travelplatform.user.service.AdminUserService;
import com.travelplatform.user.vo.admin.AdminRoleOptionVO;
import com.travelplatform.user.vo.admin.AdminUserVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin")
public class AdminUserController {
    private final AdminUserService adminUserService;
    public AdminUserController(AdminUserService adminUserService) { this.adminUserService = adminUserService; }

    @Operation(summary = "后台用户列表")
    @GetMapping("/users")
    public Result<PageResult<AdminUserVO>> page(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) Integer status,
                                                @RequestParam(defaultValue = "1") int pageNum,
                                                @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(adminUserService.page(keyword, status, pageNum, pageSize));
    }
    @Operation(summary = "后台用户详情")
    @GetMapping("/users/{id}")
    public Result<AdminUserVO> detail(@PathVariable Long id) { return Result.success(adminUserService.detail(id)); }
    @Operation(summary = "后台修改用户状态")
    @PutMapping("/users/{id}/status")
    public Result<Void> status(@PathVariable Long id, @Valid @RequestBody AdminUserStatusRequest request) {
        adminUserService.updateStatus(id, request); return Result.success();
    }
    @Operation(summary = "后台修改用户角色")
    @PutMapping("/users/{id}/roles")
    public Result<Void> roles(@PathVariable Long id, @Valid @RequestBody AdminUserRolesRequest request) {
        adminUserService.updateRoles(id, request); return Result.success();
    }
    @Operation(summary = "后台角色选项")
    @GetMapping("/roles")
    public Result<List<AdminRoleOptionVO>> roles() { return Result.success(adminUserService.roles()); }
}
