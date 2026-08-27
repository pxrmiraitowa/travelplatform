package com.travelplatform.contenttrip.dto.tripplan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;
import java.time.LocalDate;
import java.util.List;

public class AiTripPlanPreviewRequest {

    @NotBlank(message = "目的地不能为空")
    @Size(max = 50, message = "目的地长度不能超过50位")
    private String destination;

    @Min(value = 1, message = "停留天数至少为1天")
    @Max(value = 10, message = "停留天数不能超过10天")
    private Integer totalDays;

    private LocalDate startDate;

    @Size(max = 8, message = "偏好数量不能超过8个")
    private List<@NotBlank(message = "偏好不能为空") @Size(max = 20, message = "偏好长度不能超过20位") String> preferences;

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
}
