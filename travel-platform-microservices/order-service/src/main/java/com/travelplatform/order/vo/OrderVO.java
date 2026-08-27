package com.travelplatform.order.vo;

import com.travelplatform.order.entity.Order;
import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderVO {
    private Long id;
    private String orderNo;
    private String productType;
    private Long productId;
    private String productName;
    private String productSummary;
    private BigDecimal unitPrice;
    private Integer quantity;
    private BigDecimal totalAmount;
    private Integer orderStatus;
    private LocalDate travelDate;
    private String contactName;
    private String contactPhone;
    private LocalDateTime paidAt;
    private String refundReason;
    private LocalDateTime refundedAt;
    private LocalDateTime createdAt;

    public static OrderVO from(Order order) {
        OrderVO vo = new OrderVO();
        vo.id = order.getId(); vo.orderNo = order.getOrderNo(); vo.productType = order.getBizType();
        vo.productId = order.getBizId(); vo.productName = order.getProductName();
        vo.productSummary = order.getProductSummary(); vo.unitPrice = order.getUnitPrice();
        vo.quantity = order.getQuantity(); vo.totalAmount = order.getTotalAmount();
        vo.orderStatus = order.getOrderStatus(); vo.travelDate = order.getTravelDate();
        vo.contactName = order.getContactName(); vo.contactPhone = order.getContactPhone();
        vo.paidAt = order.getPaidAt(); vo.createdAt = order.getCreatedAt();
        vo.refundReason = order.getRefundReason(); vo.refundedAt = order.getRefundedAt();
        return vo;
    }
    public Long getId() { return id; }
    public String getOrderNo() { return orderNo; }
    public String getProductType() { return productType; }
    public String getBizType() { return productType; }
    public Long getProductId() { return productId; }
    public String getProductName() { return productName; }
    public String getSummaryTitle() { return productName; }
    public String getProductSummary() { return productSummary; }
    public String getSummarySubtitle() { return productSummary; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public Integer getQuantity() { return quantity; }
    public BigDecimal getTotalAmount() { return totalAmount; }
    public BigDecimal getOriginalAmount() { return totalAmount; }
    public BigDecimal getDiscountAmount() { return BigDecimal.ZERO; }
    public Integer getOrderStatus() { return orderStatus; }
    public LocalDate getTravelDate() { return travelDate; }
    public String getContactName() { return contactName; }
    public String getContactPhone() { return contactPhone; }
    public LocalDateTime getPaidAt() { return paidAt; }
    public String getRefundReason() { return refundReason; }
    public LocalDateTime getRefundedAt() { return refundedAt; }
    public LocalDateTime getCreatedAt() { return createdAt; }
    public LocalDateTime getCreateTime() { return createdAt; }
    public boolean isReviewed() { return false; }
}
