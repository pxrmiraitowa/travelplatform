package com.travelplatform.vo.review;

import java.time.LocalDate;

public class ReviewableOrderVO {

    private Long orderId;
    private String orderNo;
    private String bizType;
    private LocalDate travelDate;
    private String summaryTitle;
    private String summarySubtitle;

    public Long getOrderId() {
        return orderId;
    }

    public void setOrderId(Long orderId) {
        this.orderId = orderId;
    }

    public String getOrderNo() {
        return orderNo;
    }

    public void setOrderNo(String orderNo) {
        this.orderNo = orderNo;
    }

    public String getBizType() {
        return bizType;
    }

    public void setBizType(String bizType) {
        this.bizType = bizType;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getSummaryTitle() {
        return summaryTitle;
    }

    public void setSummaryTitle(String summaryTitle) {
        this.summaryTitle = summaryTitle;
    }

    public String getSummarySubtitle() {
        return summarySubtitle;
    }

    public void setSummarySubtitle(String summarySubtitle) {
        this.summarySubtitle = summarySubtitle;
    }
}
