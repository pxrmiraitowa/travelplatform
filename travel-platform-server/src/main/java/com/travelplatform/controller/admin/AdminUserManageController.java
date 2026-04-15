package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.admin.user.AdminUserRoleUpdateRequest;
import com.travelplatform.dto.admin.user.AdminUserStatusUpdateRequest;
import com.travelplatform.service.admin.AdminUserManageService;
import com.travelplatform.vo.admin.user.AdminRoleOptionVO;
import com.travelplatform.vo.admin.user.AdminUserDetailVO;
import com.travelplatform.vo.admin.user.AdminUserListItemVO;
import com.travelplatform.vo.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import java.util.List;

@RestController
@RequestMapping("/api/admin")
public class AdminUserManageController {

    private final AdminUserManageService adminUserManageService;

    public AdminUserManageController(AdminUserManageService adminUserManageService) {
        this.adminUserManageService = adminUserManageService;
    }

    @Operation(summary = "后台用户列表")
    @GetMapping("/users")
    public Result<PageResult<AdminUserListItemVO>> listUsers(@RequestParam(required = false) String keyword,
                                                             @RequestParam(required = false) Integer status,
                                                             @RequestParam(required = false) Integer pageNum,
                                                             @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminUserManageService.listUsers(keyword, status, pageNum, pageSize));
    }

    @Operation(summary = "后台用户详情")
    @GetMapping("/users/{id}")
    public Result<AdminUserDetailVO> getUserDetail(@PathVariable Long id) {
        return Result.success(adminUserManageService.getUserDetail(id));
    }

    @Operation(summary = "后台修改用户状态")
    @PutMapping("/users/{id}/status")
    public Result<Void> updateUserStatus(@PathVariable Long id,
                                         @Valid @RequestBody AdminUserStatusUpdateRequest request) {
        adminUserManageService.updateUserStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "后台修改用户角色")
    @PutMapping("/users/{id}/roles")
    public Result<Void> updateUserRoles(@PathVariable Long id,
                                        @Valid @RequestBody AdminUserRoleUpdateRequest request) {
        adminUserManageService.updateUserRoles(id, request);
        return Result.success();
    }

    @Operation(summary = "后台角色选项")
    @GetMapping("/roles")
    public Result<List<AdminRoleOptionVO>> listRoles() {
        return Result.success(adminUserManageService.listRoles());
    }
}
