package com.travelplatform.vo.price;

import java.math.BigDecimal;

public class CompareItemVO {

    private Long productId;
    private String productName;
    private String subTitle;
    private BigDecimal price;
    private boolean currentProduct;
    private boolean lowestPrice;
    private String highlightText;

    public Long getProductId() {
        return productId;
    }

    public void setProductId(Long productId) {
        this.productId = productId;
    }

    public String getProductName() {
        return productName;
    }

    public void setProductName(String productName) {
        this.productName = productName;
    }

    public String getSubTitle() {
        return subTitle;
    }

    public void setSubTitle(String subTitle) {
        this.subTitle = subTitle;
    }

    public BigDecimal getPrice() {
        return price;
    }

    public void setPrice(BigDecimal price) {
        this.price = price;
    }

    public boolean isCurrentProduct() {
        return currentProduct;
    }

    public void setCurrentProduct(boolean currentProduct) {
        this.currentProduct = currentProduct;
    }

    public boolean isLowestPrice() {
        return lowestPrice;
    }

    public void setLowestPrice(boolean lowestPrice) {
        this.lowestPrice = lowestPrice;
    }

    public String getHighlightText() {
        return highlightText;
    }

    public void setHighlightText(String highlightText) {
        this.highlightText = highlightText;
    }
}
