package com.travelplatform.order.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.service.OrderService;
import com.travelplatform.order.vo.ReviewContextVO;
import io.swagger.v3.oas.annotations.Operation;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/orders")
public class InternalOrderController {
    private final OrderService orderService;

    public InternalOrderController(OrderService orderService) { this.orderService = orderService; }

    @Operation(summary = "订单评价上下文")
    @GetMapping("/{orderId}/review-context")
    public Result<ReviewContextVO> reviewContext(@PathVariable Long orderId, @RequestParam Long userId) {
        return Result.success(orderService.reviewContext(orderId, userId));
    }

    @Operation(summary = "可评价订单")
    @GetMapping("/reviewable")
    public Result<PageResult<ReviewContextVO>> reviewable(@RequestParam Long userId,
                                                          @RequestParam(defaultValue = "1") int pageNum,
                                                          @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.reviewable(userId, Math.max(1, pageNum),
                Math.min(100, Math.max(1, pageSize))));
    }
}
