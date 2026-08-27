package com.travelplatform.contenttrip.vo.tripplan;

import java.time.LocalDate;
import java.util.List;

public class AiTripPlanPreviewVO {

    private String planName;
    private String destination;
    private Integer totalDays;
    private LocalDate startDate;
    private List<String> preferences;
    private String sourceType;
    private String generationMode;
    private List<AiTripPlanPreviewDayVO> days;

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public String getGenerationMode() {
        return generationMode;
    }

    public void setGenerationMode(String generationMode) {
        this.generationMode = generationMode;
    }

    public List<AiTripPlanPreviewDayVO> getDays() {
        return days;
    }

    public void setDays(List<AiTripPlanPreviewDayVO> days) {
        this.days = days;
    }
}
