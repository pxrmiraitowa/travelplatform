package com.travelplatform.order.dto.admin;

import jakarta.validation.constraints.NotNull;

public class AdminOrderStatusUpdateRequest {

    @NotNull(message = "订单状态不能为空")
    private Integer orderStatus;

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }
}
