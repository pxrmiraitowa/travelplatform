package com.travelplatform.controller.admin;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.admin.order.AdminOrderStatusUpdateRequest;
import com.travelplatform.service.admin.AdminOrderManageService;
import com.travelplatform.vo.admin.order.AdminOrderDetailVO;
import com.travelplatform.vo.admin.order.AdminOrderListItemVO;
import com.travelplatform.vo.common.PageResult;
import io.swagger.v3.oas.annotations.Operation;
import jakarta.validation.Valid;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/admin/orders")
public class AdminOrderManageController {

    private final AdminOrderManageService adminOrderManageService;

    public AdminOrderManageController(AdminOrderManageService adminOrderManageService) {
        this.adminOrderManageService = adminOrderManageService;
    }

    @Operation(summary = "后台订单列表")
    @GetMapping
    public Result<PageResult<AdminOrderListItemVO>> listOrders(@RequestParam(required = false) String keyword,
                                                               @RequestParam(required = false) String bizType,
                                                               @RequestParam(required = false) Integer status,
                                                               @RequestParam(required = false) Integer pageNum,
                                                               @RequestParam(required = false) Integer pageSize) {
        return Result.success(adminOrderManageService.listOrders(keyword, bizType, status, pageNum, pageSize));
    }

    @Operation(summary = "后台订单详情")
    @GetMapping("/{id}")
    public Result<AdminOrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(adminOrderManageService.getOrderDetail(id));
    }

    @Operation(summary = "后台修改订单状态")
    @PutMapping("/{id}/status")
    public Result<Void> updateOrderStatus(@PathVariable Long id,
                                          @Valid @RequestBody AdminOrderStatusUpdateRequest request) {
        adminOrderManageService.updateOrderStatus(id, request);
        return Result.success();
    }

    @Operation(summary = "后台取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        adminOrderManageService.cancelOrder(id);
        return Result.success();
    }
}
