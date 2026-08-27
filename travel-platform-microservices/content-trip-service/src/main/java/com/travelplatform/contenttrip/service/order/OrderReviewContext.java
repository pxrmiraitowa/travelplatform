package com.travelplatform.contenttrip.service.order;

import java.time.LocalDate;

public class OrderReviewContext {

    private Long orderId;
    private String orderNo;
    private Long userId;
    private String bizType;
    private Long bizId;
    private LocalDate travelDate;
    private String summaryTitle;
    private String summarySubtitle;
    private boolean completed;

    public Long getOrderId() { return orderId; }
    public void setOrderId(Long orderId) { this.orderId = orderId; }
    public String getOrderNo() { return orderNo; }
    public void setOrderNo(String orderNo) { this.orderNo = orderNo; }
    public Long getUserId() { return userId; }
    public void setUserId(Long userId) { this.userId = userId; }
    public String getBizType() { return bizType; }
    public void setBizType(String bizType) { this.bizType = bizType; }
    public Long getBizId() { return bizId; }
    public void setBizId(Long bizId) { this.bizId = bizId; }
    public LocalDate getTravelDate() { return travelDate; }
    public void setTravelDate(LocalDate travelDate) { this.travelDate = travelDate; }
    public String getSummaryTitle() { return summaryTitle; }
    public void setSummaryTitle(String summaryTitle) { this.summaryTitle = summaryTitle; }
    public String getSummarySubtitle() { return summarySubtitle; }
    public void setSummarySubtitle(String summarySubtitle) { this.summarySubtitle = summarySubtitle; }
    public boolean isCompleted() { return completed; }
    public void setCompleted(boolean completed) { this.completed = completed; }
}
