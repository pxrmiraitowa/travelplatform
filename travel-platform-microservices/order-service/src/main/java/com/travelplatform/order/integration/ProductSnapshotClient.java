package com.travelplatform.order.integration;

import com.travelplatform.order.dto.OrderCreateRequest;

public interface ProductSnapshotClient {
    ProductSnapshot getSnapshot(OrderCreateRequest request);
    CouponSettlement settleCoupon(String productType, Long couponId, java.math.BigDecimal originalAmount);
}
