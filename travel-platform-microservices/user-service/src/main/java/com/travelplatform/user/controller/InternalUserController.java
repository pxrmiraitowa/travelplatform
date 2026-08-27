package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.user.service.UserService;
import com.travelplatform.user.vo.BasicUserVO;
import io.swagger.v3.oas.annotations.Operation;
import java.util.List;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserController {

    private final UserService userService;

    public InternalUserController(UserService userService) {
        this.userService = userService;
    }

    @Operation(summary = "批量查询用户基础信息")
    @GetMapping("/basic")
    public Result<List<BasicUserVO>> listBasicUsers(@RequestParam("ids") List<Long> ids) {
        return Result.success(userService.listBasicUsers(ids));
    }
}
