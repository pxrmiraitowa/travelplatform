package com.travelplatform.product.vo.price;

import java.math.BigDecimal;
import java.util.List;

public class PriceCompareVO {

    private String productType;
    private Long productId;
    private BigDecimal currentPrice;
    private BigDecimal lowestPrice;
    private boolean lowest;
    private BigDecimal priceDiff;
    private String lowPriceLabel;
    private List<CompareItemVO> compareItems;
    private List<CouponVO> couponList;

    public String getProductType() {
        return productType;
    }

    public void setProductType(String productType) {
        this.productType = productType;
    }

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }

    public void setCurrentPrice(BigDecimal currentPrice) {
        this.currentPrice = currentPrice;
    }

    public BigDecimal getLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(BigDecimal lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public boolean isLowest() {
        return lowest;
    }

    public void setLowest(boolean lowest) {
        this.lowest = lowest;
    }

    public BigDecimal getPriceDiff() {
        return priceDiff;
    }

    public void setPriceDiff(BigDecimal priceDiff) {
        this.priceDiff = priceDiff;
    }

    public String getLowPriceLabel() {
        return lowPriceLabel;
    }

    public void setLowPriceLabel(String lowPriceLabel) {
        this.lowPriceLabel = lowPriceLabel;
    }

    public List<CompareItemVO> getCompareItems() {
        return compareItems;
    }

    public void setCompareItems(List<CompareItemVO> compareItems) {
        this.compareItems = compareItems;
    }

    public List<CouponVO> getCouponList() {
        return couponList;
    }

    public void setCouponList(List<CouponVO> couponList) {
        this.couponList = couponList;
    }
}
