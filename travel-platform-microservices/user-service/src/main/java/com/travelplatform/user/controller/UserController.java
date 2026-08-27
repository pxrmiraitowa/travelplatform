package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.user.dto.user.UpdateUserProfileRequest;
import com.travelplatform.user.service.UserService;
import com.travelplatform.user.vo.CurrentUserVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/users")
public class UserController {

    private final UserService userService;

    public UserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "获取当前用户信息")
    @GetMapping("/me")
    public Result<CurrentUserVO> getCurrentUser() {
        return Result.success(userService.getCurrentUser());
    }

    @Operation(summary = "修改个人信息")
    @PutMapping("/me")
    public Result<CurrentUserVO> updateCurrentUser(@Valid @RequestBody UpdateUserProfileRequest request) {
        return Result.success(userService.updateCurrentUser(request));
    }
}
