package com.travelplatform.vo.order;

import com.travelplatform.vo.review.ReviewVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class OrderDetailVO {

    private Long id;
    private String orderNo;
    private String bizType;
    private Long bizId;
    private Integer orderStatus;
    private BigDecimal originalAmount;
    private BigDecimal discountAmount;
    private BigDecimal totalAmount;
    private String couponName;
    private String contactName;
    private String contactPhone;
    private LocalDate travelDate;
    private String remark;
    private LocalDateTime createTime;
    private Boolean reviewed;
    private ReviewVO reviewInfo;
    private OrderFlightVO flightInfo;
    private OrderTrainVO trainInfo;
    private OrderHotelVO hotelInfo;
    private OrderTourVO tourInfo;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
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

    public Long getBizId() {
        return bizId;
    }

    public void setBizId(Long bizId) {
        this.bizId = bizId;
    }

    public Integer getOrderStatus() {
        return orderStatus;
    }

    public void setOrderStatus(Integer orderStatus) {
        this.orderStatus = orderStatus;
    }

    public BigDecimal getOriginalAmount() {
        return originalAmount;
    }

    public void setOriginalAmount(BigDecimal originalAmount) {
        this.originalAmount = originalAmount;
    }

    public BigDecimal getDiscountAmount() {
        return discountAmount;
    }

    public void setDiscountAmount(BigDecimal discountAmount) {
        this.discountAmount = discountAmount;
    }

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
    }

    public String getCouponName() {
        return couponName;
    }

    public void setCouponName(String couponName) {
        this.couponName = couponName;
    }

    public String getContactName() {
        return contactName;
    }

    public void setContactName(String contactName) {
        this.contactName = contactName;
    }

    public String getContactPhone() {
        return contactPhone;
    }

    public void setContactPhone(String contactPhone) {
        this.contactPhone = contactPhone;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public Boolean getReviewed() {
        return reviewed;
    }

    public void setReviewed(Boolean reviewed) {
        this.reviewed = reviewed;
    }

    public ReviewVO getReviewInfo() {
        return reviewInfo;
    }

    public void setReviewInfo(ReviewVO reviewInfo) {
        this.reviewInfo = reviewInfo;
    }

    public OrderFlightVO getFlightInfo() {
        return flightInfo;
    }

    public void setFlightInfo(OrderFlightVO flightInfo) {
        this.flightInfo = flightInfo;
    }

    public OrderTrainVO getTrainInfo() {
        return trainInfo;
    }

    public void setTrainInfo(OrderTrainVO trainInfo) {
        this.trainInfo = trainInfo;
    }

    public OrderHotelVO getHotelInfo() {
        return hotelInfo;
    }

    public void setHotelInfo(OrderHotelVO hotelInfo) {
        this.hotelInfo = hotelInfo;
    }

    public OrderTourVO getTourInfo() {
        return tourInfo;
    }

    public void setTourInfo(OrderTourVO tourInfo) {
        this.tourInfo = tourInfo;
    }
}
