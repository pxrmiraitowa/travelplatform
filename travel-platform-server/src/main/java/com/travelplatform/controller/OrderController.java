package com.travelplatform.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.dto.order.FlightOrderCreateRequest;
import com.travelplatform.dto.order.HotelOrderCreateRequest;
import com.travelplatform.dto.order.TourOrderCreateRequest;
import com.travelplatform.dto.order.TrainOrderCreateRequest;
import com.travelplatform.service.order.OrderService;
import com.travelplatform.vo.common.PageResult;
import com.travelplatform.vo.order.OrderDetailVO;
import com.travelplatform.vo.order.OrderListItemVO;
import io.swagger.v3.oas.annotations.Operation;
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

    public OrderController(OrderService orderService) {
        this.orderService = orderService;
    }

    @Operation(summary = "提交机票订单")
    @PostMapping("/flights")
    public Result<OrderDetailVO> createFlightOrder(@Valid @RequestBody FlightOrderCreateRequest request) {
        return Result.success(orderService.createFlightOrder(request));
    }

    @Operation(summary = "提交火车票订单")
    @PostMapping("/trains")
    public Result<OrderDetailVO> createTrainOrder(@Valid @RequestBody TrainOrderCreateRequest request) {
        return Result.success(orderService.createTrainOrder(request));
    }

    @Operation(summary = "提交酒店订单")
    @PostMapping("/hotels")
    public Result<OrderDetailVO> createHotelOrder(@Valid @RequestBody HotelOrderCreateRequest request) {
        return Result.success(orderService.createHotelOrder(request));
    }

    @Operation(summary = "提交旅游产品订单")
    @PostMapping("/tours")
    public Result<OrderDetailVO> createTourOrder(@Valid @RequestBody TourOrderCreateRequest request) {
        return Result.success(orderService.createTourOrder(request));
    }

    @Operation(summary = "查询当前用户订单列表")
    @GetMapping
    public Result<PageResult<OrderListItemVO>> listOrders(@RequestParam(required = false) String bizType,
                                                          @RequestParam(required = false) Integer status,
                                                          @RequestParam(required = false) Integer pageNum,
                                                          @RequestParam(required = false) Integer pageSize) {
        return Result.success(orderService.listCurrentUserOrders(bizType, status, pageNum, pageSize));
    }

    @Operation(summary = "查询订单详情")
    @GetMapping("/{id}")
    public Result<OrderDetailVO> getOrderDetail(@PathVariable Long id) {
        return Result.success(orderService.getCurrentUserOrderDetail(id));
    }

    @Operation(summary = "取消订单")
    @PostMapping("/{id}/cancel")
    public Result<Void> cancelOrder(@PathVariable Long id) {
        orderService.cancelCurrentUserOrder(id);
        return Result.success();
    }
}
