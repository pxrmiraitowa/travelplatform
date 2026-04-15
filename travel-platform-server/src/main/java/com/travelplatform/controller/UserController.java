package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.user.UpdateUserProfileRequest;
import com.travelplatform.service.user.UserService;
import com.travelplatform.vo.user.CurrentUserVO;
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
