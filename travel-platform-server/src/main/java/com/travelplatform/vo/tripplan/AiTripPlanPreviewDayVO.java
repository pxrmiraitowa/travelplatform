package com.travelplatform.vo.tripplan;

import java.util.List;

public class AiTripPlanPreviewDayVO {

    private Integer dayNo;

    private String destination;

    private String reason;

    private List<AiTripPlanAttractionVO> attractions;

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

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }

    public List<AiTripPlanAttractionVO> getAttractions() {
        return attractions;
    }

    public void setAttractions(List<AiTripPlanAttractionVO> attractions) {
        this.attractions = attractions;
    }
}
