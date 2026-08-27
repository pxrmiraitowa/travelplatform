package com.travelplatform.user.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.user.mapper.UserMapper;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/users")
public class InternalUserStatsController {

    private final UserMapper userMapper;

    public InternalUserStatsController(UserMapper userMapper) {
        this.userMapper = userMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        return Result.success(Map.of("userCount", userMapper.selectCount(null)));
    }
}
