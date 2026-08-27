package com.travelplatform.contenttrip.dto.tripplan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

public class TripPlanItemUpdateRequest {

    @Min(value = 1, message = "行程天数必须从第1天开始")
    @Max(value = 60, message = "行程天数不能超过60天")
    private Integer dayNo;

    @NotBlank(message = "目的地不能为空")
    @Size(max = 100, message = "目的地长度不能超过100位")
    private String destination;

    @Size(max = 100, message = "酒店名称长度不能超过100位")
    private String hotel;

    @Size(max = 50, message = "出行方式长度不能超过50位")
    private String transportType;

    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;

    public Integer getDayNo() {
        return dayNo;
    }

    public void setDayNo(Integer dayNo) {
        this.dayNo = dayNo;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getHotel() {
        return hotel;
    }

    public void setHotel(String hotel) {
        this.hotel = hotel;
    }

    public String getTransportType() {
        return transportType;
    }

    public void setTransportType(String transportType) {
        this.transportType = transportType;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
