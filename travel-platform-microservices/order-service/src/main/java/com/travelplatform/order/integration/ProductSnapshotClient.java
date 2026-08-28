package com.travelplatform.order.integration;

import com.travelplatform.order.dto.OrderCreateRequest;

public interface ProductSnapshotClient {
    ProductSnapshot getSnapshot(OrderCreateRequest request);
    CouponSettlement settleCoupon(String productType, Long couponId, java.math.BigDecimal originalAmount);
    void deductStock(String productType, Long productId, Long variantId, String variantName, int quantity);
    void restoreStock(String productType, Long productId, Long variantId, String variantName, int quantity);
}
