package com.travelplatform.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class FlightOrderCreateRequest {

    @NotNull(message = "航班不能为空")
    private Long flightId;

    @NotNull(message = "乘机人不能为空")
    private Long contactId;

    private Long couponId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    public Long getFlightId() {
        return flightId;
    }

    public void setFlightId(Long flightId) {
        this.flightId = flightId;
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
