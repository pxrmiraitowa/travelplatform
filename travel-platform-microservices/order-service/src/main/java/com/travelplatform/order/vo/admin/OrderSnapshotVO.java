package com.travelplatform.order.vo.admin;

import java.math.BigDecimal;
import java.time.LocalDate;

public class OrderSnapshotVO {

    private Long productId;
    private String productName;
    private String productSummary;
    private BigDecimal unitPrice;
    private Integer quantity;
    private LocalDate travelDate;

    public Long getProductId() { return productId; }
    public void setProductId(Long productId) { this.productId = productId; }
    public String getProductName() { return productName; }
    public void setProductName(String productName) { this.productName = productName; }
    public String getProductSummary() { return productSummary; }
    public void setProductSummary(String productSummary) { this.productSummary = productSummary; }
    public BigDecimal getUnitPrice() { return unitPrice; }
    public void setUnitPrice(BigDecimal unitPrice) { this.unitPrice = unitPrice; }
    public Integer getQuantity() { return quantity; }
    public void setQuantity(Integer quantity) { this.quantity = quantity; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }

    public String getFlightNo() { return productName; }
    public String getAirlineName() { return productSummary; }
    public String getTrainNo() { return productName; }
    public String getSeatType() { return productSummary; }
    public String getHotelName() { return productName; }
    public String getRoomName() { return productSummary; }
    public String getAddress() { return productSummary; }
    public LocalDate getCheckInDate() { return travelDate; }
    public LocalDate getCheckOutDate() { return travelDate; }
    public String getPackageName() { return productName; }
    public String getDestination() { return productSummary; }
    public String getDepartureCity() { return productSummary; }
}
