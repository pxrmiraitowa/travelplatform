package com.travelplatform.dto.tripplan;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.Size;

import java.time.LocalDate;
import java.util.List;

public class AiTripPlanSaveRequest {

    @NotBlank(message = "计划名称不能为空")
    @Size(max = 100, message = "计划名称长度不能超过100位")
    private String planName;

    @NotBlank(message = "目的地不能为空")
    @Size(max = 50, message = "目的地长度不能超过50位")
    private String destination;

    private Integer totalDays;

    private LocalDate startDate;

    @Size(max = 8, message = "偏好数量不能超过8个")
    private List<@NotBlank(message = "偏好不能为空") @Size(max = 20, message = "偏好长度不能超过20位") String> preferences;

    @NotEmpty(message = "预览数据不能为空")
    private List<@Valid AiTripPlanSaveDayRequest> days;

    public String getPlanName() {
        return planName;
    }

    public void setPlanName(String planName) {
        this.planName = planName;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
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

    public List<String> getPreferences() {
        return preferences;
    }

    public void setPreferences(List<String> preferences) {
        this.preferences = preferences;
    }

    public List<AiTripPlanSaveDayRequest> getDays() {
        return days;
    }

    public void setDays(List<AiTripPlanSaveDayRequest> days) {
        this.days = days;
    }
}
