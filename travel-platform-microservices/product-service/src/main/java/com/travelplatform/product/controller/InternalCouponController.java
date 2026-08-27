package com.travelplatform.product.controller;

import com.travelplatform.common.result.Result;
import com.travelplatform.product.service.coupon.CouponSettlementService;
import com.travelplatform.product.vo.coupon.CouponSettlementVO;
import io.swagger.v3.oas.annotations.Operation;
import java.math.BigDecimal;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/internal/coupons")
public class InternalCouponController {
    private final CouponSettlementService couponSettlementService;

    public InternalCouponController(CouponSettlementService couponSettlementService) {
        this.couponSettlementService = couponSettlementService;
    }

    @Operation(summary = "优惠券结算")
    @GetMapping("/settlement")
    public Result<CouponSettlementVO> settlement(@RequestParam String productType,
                                                 @RequestParam(required = false) Long couponId,
                                                 @RequestParam BigDecimal originalAmount) {
        return Result.success(couponSettlementService.settle(productType, couponId, originalAmount));
    }
}
