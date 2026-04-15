package com.travelplatform.dto.order;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import org.springframework.format.annotation.DateTimeFormat;

import java.time.LocalDate;

public class TourOrderCreateRequest {

    @NotNull(message = "鏃呮父浜у搧涓嶈兘涓虹┖")
    private Long tourPackageId;

    @NotNull(message = "鍑鸿鏃ユ湡涓嶈兘涓虹┖")
    @DateTimeFormat(pattern = "yyyy-MM-dd")
    private LocalDate travelDate;

    @NotNull(message = "鍑鸿浜轰笉鑳戒负绌?")
    private Long contactId;

    private Long couponId;

    @Size(max = 255, message = "澶囨敞闀垮害涓嶈兘瓒呰繃255涓瓧绗?")
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
