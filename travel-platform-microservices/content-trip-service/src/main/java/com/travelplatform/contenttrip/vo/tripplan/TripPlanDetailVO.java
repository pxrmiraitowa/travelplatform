package com.travelplatform.contenttrip.vo.tripplan;

import java.time.LocalDate;
import java.time.LocalDateTime;
import java.util.List;

public class TripPlanDetailVO {

    private Long id;
    private String planName;
    private Integer totalDays;
    private LocalDate startDate;
    private String remark;
    private String sourceType;
    private LocalDateTime createTime;
    private List<TripPlanItemVO> items;

    public Long getId() {
        return id;
    }

    public void setId(Long id) {
        this.id = id;
    }

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

    public String getSourceType() {
        return sourceType;
    }

    public void setSourceType(String sourceType) {
        this.sourceType = sourceType;
    }

    public LocalDateTime getCreateTime() {
        return createTime;
    }

    public void setCreateTime(LocalDateTime createTime) {
        this.createTime = createTime;
    }

    public List<TripPlanItemVO> getItems() {
        return items;
    }

    public void setItems(List<TripPlanItemVO> items) {
        this.items = items;
    }
}
