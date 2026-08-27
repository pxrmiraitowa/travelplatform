package com.travelplatform.product.service.coupon.impl;

import com.travelplatform.common.exception.BusinessException;
import com.travelplatform.common.result.ResultCode;
import com.travelplatform.product.entity.Coupon;
import com.travelplatform.product.mapper.CouponMapper;
import com.travelplatform.product.service.coupon.CouponSettlementService;
import com.travelplatform.product.vo.coupon.CouponSettlementVO;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Locale;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;

@Service
public class CouponSettlementServiceImpl implements CouponSettlementService {
    private final CouponMapper couponMapper;

    public CouponSettlementServiceImpl(CouponMapper couponMapper) {
        this.couponMapper = couponMapper;
    }

    @Override
    public CouponSettlementVO settle(String productType, Long couponId, BigDecimal originalAmount) {
        if (couponId == null) {
            return withoutCoupon(originalAmount);
        }
        if (!StringUtils.hasText(productType)) {
            throw badRequest("商品类型不能为空");
        }
        if (originalAmount == null || originalAmount.compareTo(BigDecimal.ZERO) < 0) {
            throw badRequest("订单金额不合法");
        }

        Coupon coupon = couponMapper.selectById(couponId);
        if (coupon == null || !Integer.valueOf(1).equals(coupon.getStatus())) {
            throw badRequest("优惠券不存在或不可用");
        }

        String normalizedType = productType.trim().toUpperCase(Locale.ROOT);
        if (!normalizedType.equals(coupon.getProductType())) {
            throw badRequest("优惠券不适用于当前订单");
        }

        LocalDateTime now = LocalDateTime.now();
        if ((coupon.getStartTime() != null && coupon.getStartTime().isAfter(now))
                || (coupon.getEndTime() != null && coupon.getEndTime().isBefore(now))) {
            throw badRequest("优惠券不在有效期内");
        }

        BigDecimal threshold = coupon.getThresholdAmount() == null ? BigDecimal.ZERO : coupon.getThresholdAmount();
        if (originalAmount.compareTo(threshold) < 0) {
            throw badRequest("订单金额未达到优惠券使用门槛");
        }

        BigDecimal discount = coupon.getDiscountAmount() == null ? BigDecimal.ZERO : coupon.getDiscountAmount();
        if (discount.compareTo(originalAmount) > 0) {
            discount = originalAmount;
        }
        BigDecimal payableAmount = originalAmount.subtract(discount);

        CouponSettlementVO vo = new CouponSettlementVO();
        vo.setCouponId(coupon.getId());
        vo.setCouponName(coupon.getCouponName());
        vo.setOriginalAmount(originalAmount);
        vo.setDiscountAmount(discount);
        vo.setPayableAmount(payableAmount);
        vo.setUsed(true);
        return vo;
    }

    private CouponSettlementVO withoutCoupon(BigDecimal originalAmount) {
        BigDecimal amount = originalAmount == null ? BigDecimal.ZERO : originalAmount;
        CouponSettlementVO vo = new CouponSettlementVO();
        vo.setOriginalAmount(amount);
        vo.setDiscountAmount(BigDecimal.ZERO);
        vo.setPayableAmount(amount);
        vo.setUsed(false);
        return vo;
    }

    private BusinessException badRequest(String message) {
        return new BusinessException(ResultCode.BAD_REQUEST.getCode(), message);
    }
}
