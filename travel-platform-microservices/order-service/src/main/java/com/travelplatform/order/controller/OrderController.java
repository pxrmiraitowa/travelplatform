package com.travelplatform.order.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.dto.OrderCreateRequest;
import com.travelplatform.order.dto.OrderRefundRequest;
import com.travelplatform.order.security.CurrentUserProvider;
import com.travelplatform.order.service.OrderService;
import com.travelplatform.order.vo.OrderVO;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/orders")
public class OrderController {
    private final OrderService orderService;
    private final CurrentUserProvider currentUserProvider;

    public OrderController(OrderService orderService, CurrentUserProvider currentUserProvider) {
        this.orderService = orderService;
        this.currentUserProvider = currentUserProvider;
    }

    @Operation(summary = "创建订单")
    @PostMapping
    public Result<OrderVO> create(HttpServletRequest servletRequest,
                                  @Valid @RequestBody OrderCreateRequest request) {
        return Result.success(orderService.create(currentUserProvider.getCurrentUserId(servletRequest), request));
    }

    @Operation(summary = "我的订单列表")
    @GetMapping
    public Result<PageResult<OrderVO>> page(HttpServletRequest request,
                                            @RequestParam(required = false) String bizType,
                                            @RequestParam(required = false) Integer status,
                                            @RequestParam(defaultValue = "1") int pageNum,
                                            @RequestParam(defaultValue = "10") int pageSize) {
        return Result.success(orderService.page(currentUserProvider.getCurrentUserId(request), bizType, status,
                normalizePage(pageNum), normalizeSize(pageSize)));
    }

    @Operation(summary = "订单详情")
    @GetMapping("/{id}")
    public Result<OrderVO> detail(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.detail(currentUserProvider.getCurrentUserId(request), id));
    }

    @Operation(summary = "模拟支付")
    @PostMapping("/{id}/pay")
    public Result<OrderVO> pay(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.pay(currentUserProvider.getCurrentUserId(request), id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<OrderVO> cancel(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.cancel(currentUserProvider.getCurrentUserId(request), id));
    }

    @Operation(summary = "模拟退款")
    @PostMapping("/{id}/refund")
    public Result<OrderVO> refund(HttpServletRequest request, @PathVariable Long id,
                                  @Valid @RequestBody OrderRefundRequest refundRequest) {
        return Result.success(orderService.refund(currentUserProvider.getCurrentUserId(request), id, refundRequest));
    }

    @Operation(summary = "演示用完成订单")
    @PostMapping("/{id}/complete")
    public Result<OrderVO> complete(HttpServletRequest request, @PathVariable Long id) {
        return Result.success(orderService.complete(currentUserProvider.getCurrentUserId(request), id));
    }

    private int normalizePage(int value) { return Math.max(1, value); }
    private int normalizeSize(int value) { return Math.min(100, Math.max(1, value)); }
}
