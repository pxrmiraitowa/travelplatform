package com.travelplatform.product.service.coupon;

import com.travelplatform.product.vo.coupon.CouponSettlementVO;
import java.math.BigDecimal;

public interface CouponSettlementService {
    CouponSettlementVO settle(String productType, Long couponId, BigDecimal originalAmount);
}
