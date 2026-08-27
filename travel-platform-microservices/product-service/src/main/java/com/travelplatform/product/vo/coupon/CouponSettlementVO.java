package com.travelplatform.product.vo.coupon;

import java.math.BigDecimal;

public class CouponSettlementVO {
    private Long couponId;
    private String couponName;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal payableAmount;
    private boolean used;

    public Long getCouponId() { return couponId; }
    public void setCouponId(Long couponId) { this.couponId = couponId; }
    public String getCouponName() { return couponName; }
    public void setCouponName(String couponName) { this.couponName = couponName; }
    public BigDecimal getOriginalAmount() { return originalAmount; }
    public void setOriginalAmount(BigDecimal originalAmount) { this.originalAmount = originalAmount; }
    public BigDecimal getDiscountAmount() { return discountAmount; }
    public void setDiscountAmount(BigDecimal discountAmount) { this.discountAmount = discountAmount; }
    public BigDecimal getPayableAmount() { return payableAmount; }
    public void setPayableAmount(BigDecimal payableAmount) { this.payableAmount = payableAmount; }
    public boolean isUsed() { return used; }
    public void setUsed(boolean used) { this.used = used; }
}
