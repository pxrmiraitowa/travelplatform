package com.travelplatform.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class HotelOrderCreateRequest {

    @NotNull(message = "酒店不能为空")
    private Long hotelId;

    @NotNull(message = "房型不能为空")
    private Long hotelRoomId;

    @NotNull(message = "入住日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkInDate;

    @NotNull(message = "离店日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate checkOutDate;

    @NotNull(message = "入住人不能为空")
    private Long contactId;

    private Long couponId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    public Long getHotelId() {
        return hotelId;
    }

    public void setHotelId(Long hotelId) {
        this.hotelId = hotelId;
    }

    public Long getHotelRoomId() {
        return hotelRoomId;
    }

    public void setHotelRoomId(Long hotelRoomId) {
        this.hotelRoomId = hotelRoomId;
    }

    public LocalDate getCheckInDate() {
        return checkInDate;
    }

    public void setCheckInDate(LocalDate checkInDate) {
        this.checkInDate = checkInDate;
    }

    public LocalDate getCheckOutDate() {
        return checkOutDate;
    }

    public void setCheckOutDate(LocalDate checkOutDate) {
        this.checkOutDate = checkOutDate;
    }

    public Long getContactId() {
        return contactId;
    }

    public void setContactId(Long contactId) {
        this.contactId = contactId;
    }

    public Long getCouponId() {
        return couponId;
    }

    public void setCouponId(Long couponId) {
        this.couponId = couponId;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
