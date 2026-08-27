package com.travelplatform.contenttrip.service.product;

import java.math.BigDecimal;

public class ProductSnapshot {

    private final String productType;
    private final Long productId;
    private final String productName;
    private final BigDecimal currentPrice;

    public ProductSnapshot(String productType, Long productId, String productName, BigDecimal currentPrice) {
        this.productType = productType;
        this.productId = productId;
        this.productName = productName;
        this.currentPrice = currentPrice;
    }

    public String getProductType() {
        return productType;
    }

    public Long getProductId() {
        return productId;
    }

    public String getProductName() {
        return productName;
    }

    public BigDecimal getCurrentPrice() {
        return currentPrice;
    }
}
