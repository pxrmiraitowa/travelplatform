package com.travelplatform.contenttrip.dto.tripplan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;

public class TripPlanCreateRequest {

    @NotBlank(message = "行程名称不能为空")
    @Size(max = 100, message = "行程名称长度不能超过100位")
    private String planName;

    @Min(value = 1, message = "出行总天数至少为1天")
    @Max(value = 60, message = "出行总天数不能超过60天")
    private Integer totalDays;

    private LocalDate startDate;

    @Size(max = 255, message = "备注长度不能超过255位")
    private String remark;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public Integer getTotalDays() {
        return totalDays;
    }

    public void setTotalDays(Integer totalDays) {
        this.totalDays = totalDays;
    }

    public LocalDate getStartDate() {
        return startDate;
    }

    public void setStartDate(LocalDate startDate) {
        this.startDate = startDate;
    }

    public String getRemark() {
        return remark;
    }

    public void setRemark(String remark) {
        this.remark = remark;
    }
}
