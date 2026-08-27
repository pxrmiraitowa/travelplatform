package com.travelplatform.order.service;

import com.travelplatform.common.vo.PageResult;
import com.travelplatform.order.dto.OrderCreateRequest;
import com.travelplatform.order.dto.OrderRefundRequest;
import com.travelplatform.order.vo.OrderVO;
import com.travelplatform.order.vo.ReviewContextVO;

public interface OrderService {
    OrderVO create(Long userId, OrderCreateRequest request);
    PageResult<OrderVO> page(Long userId, String bizType, Integer status, int pageNum, int pageSize);
    OrderVO detail(Long userId, Long orderId);
    OrderVO pay(Long userId, Long orderId);
    OrderVO cancel(Long userId, Long orderId);
    OrderVO refund(Long userId, Long orderId, OrderRefundRequest request);
    OrderVO complete(Long userId, Long orderId);
    ReviewContextVO reviewContext(Long orderId, Long userId);
    PageResult<ReviewContextVO> reviewable(Long userId, int pageNum, int pageSize);
}
