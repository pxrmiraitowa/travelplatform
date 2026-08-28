package com.travelplatform.product.dto.internal;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;

public class StockChangeRequest {
    @NotBlank private String productType;
    @NotNull private Long productId;
    private Long variantId;
    private String variantName;
    @NotNull @Min(1) @Max(99) private Integer quantity;

    public String getProductType() { return productType; }
    public void setProductType(String productType) { this.productType = productType; }
    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public Long getVariantId() { return variantId; }
    public void setVariantId(Long variantId) { this.variantId = variantId; }
    public String getVariantName() { return variantName; }
    public void setVariantName(String variantName) { this.variantName = variantName; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
}
