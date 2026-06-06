package com.travelplatform.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TourOrderCreateRequest {

    @NotNull(message = "旅游产品不能为空")
    private Long tourPackageId;

    @NotNull(message = "出行日期不能为空")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    @NotNull(message = "出行人不能为空")
    private Long contactId;

    private Long couponId;

    @Size(max = 255, message = "备注长度不能超过255个字符")
    private String remark;

    public Long getTourPackageId() {
        return tourPackageId;
    }

    public void setTourPackageId(Long tourPackageId) {
        this.tourPackageId = tourPackageId;
    }

    public LocalDate getTravelDate() {
        return travelDate;
    }

    public void setTravelDate(LocalDate travelDate) {
        this.travelDate = travelDate;
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
