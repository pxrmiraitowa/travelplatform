package com.travelplatform.dto.tripplan;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.util.List;

public class AiTripPlanSaveDayRequest {

    @NotNull(message = "天数不能为空")
    @Min(value = 1, message = "天数至少为1")
    @Max(value = 10, message = "天数不能超过10")
    private Integer dayNo;

    @NotEmpty(message = "每天至少包含一个景点")
    @Size(max = 5, message = "每天景点数量不能超过5个")
    private List<@NotNull(message = "景点ID不能为空") Long> attractionIds;

    @Size(max = 255, message = "推荐理由长度不能超过255位")
    private String reason;

    public Integer getDayNo() {
        return dayNo;
    }

    public void setDayNo(Integer dayNo) {
        this.dayNo = dayNo;
    }

    public List<Long> getAttractionIds() {
        return attractionIds;
    }

    public void setAttractionIds(List<Long> attractionIds) {
        this.attractionIds = attractionIds;
    }

    public String getReason() {
        return reason;
    }

    public void setReason(String reason) {
        this.reason = reason;
    }
}
