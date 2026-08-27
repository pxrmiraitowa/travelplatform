package com.travelplatform.order.integration;

import java.math.BigDecimal;

public record CouponSettlement(Long couponId, String couponName, BigDecimal originalAmount,
                               BigDecimal discountAmount, BigDecimal payableAmount, boolean used) {
}
