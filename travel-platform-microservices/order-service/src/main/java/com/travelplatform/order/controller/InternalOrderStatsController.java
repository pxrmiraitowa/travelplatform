package com.travelplatform.order.controller;

import com.baomidou.mybatisplus.core.conditions.query.LambdaQueryWrapper;
import com.travelplatform.common.result.Result;
import com.travelplatform.order.entity.Order;
import com.travelplatform.order.mapper.OrderMapper;
import java.time.LocalDateTime;
import java.util.Map;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/orders")
public class InternalOrderStatsController {

    private final OrderMapper orderMapper;

    public InternalOrderStatsController(OrderMapper orderMapper) {
        this.orderMapper = orderMapper;
    }

    @GetMapping("/stats")
    public Result<Map<String, Long>> stats() {
        long orderCount = orderMapper.selectCount(null);
        long recentOrderCount = orderMapper.selectCount(new LambdaQueryWrapper<Order>()
                .ge(Order::getCreatedAt, LocalDateTime.now().minusDays(7)));
        return Result.success(Map.of("orderCount", orderCount, "recentOrderCount", recentOrderCount));
    }
}
