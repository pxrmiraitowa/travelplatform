package com.travelplatform.vo.admin.order;

import com.travelplatform.vo.order.OrderFlightVO;
import com.travelplatform.vo.order.OrderHotelVO;
import com.travelplatform.vo.order.OrderTourVO;
import com.travelplatform.vo.order.OrderTrainVO;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.LocalDateTime;

public class AdminOrderDetailVO {

    private Long id;
    private String orderNo;
    private String bizType;
    private Long bizId;
    private Integer orderStatus;
    private BigDecimal totalAmount;
    private String contactName;
    private String contactPhone;
    private LocalDate travelDate;
    private String remark;
    private LocalDateTime createTime;
    private Long userId;
    private String username;
    private String nickname;
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

    public BigDecimal getTotalAmount() {
        return totalAmount;
    }

    public void setTotalAmount(BigDecimal totalAmount) {
        this.totalAmount = totalAmount;
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

    public Long getUserId() {
        return userId;
    }

    public void setUserId(Long userId) {
        this.userId = userId;
    }

    public String getUsername() {
        return username;
    }

    public void setUsername(String username) {
        this.username = username;
    }

    public String getNickname() {
        return nickname;
    }

    public void setNickname(String nickname) {
        this.nickname = nickname;
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
