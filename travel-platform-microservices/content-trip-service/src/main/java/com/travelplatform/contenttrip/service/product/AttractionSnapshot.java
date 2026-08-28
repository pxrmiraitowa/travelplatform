package com.travelplatform.contenttrip.service.product;

public class AttractionSnapshot {
    private Long id;
    private String city;
    private String district;
    private String attractionName;
    private String attractionType;
    private String tags;
    private String description;
    private String suggestedDuration;
    private Integer priority;
    private Integer status;

    public Long getId() { return id; }
    public void setId(Long id) { this.id = id; }
    public String getCity() { return city; }
    public void setCity(String city) { this.city = city; }
    public String getDistrict() { return district; }
    public void setDistrict(String district) { this.district = district; }
    public String getAttractionName() { return attractionName; }
    public void setAttractionName(String attractionName) { this.attractionName = attractionName; }
    public String getAttractionType() { return attractionType; }
    public void setAttractionType(String attractionType) { this.attractionType = attractionType; }
    public String getTags() { return tags; }
    public void setTags(String tags) { this.tags = tags; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSuggestedDuration() { return suggestedDuration; }
    public void setSuggestedDuration(String suggestedDuration) { this.suggestedDuration = suggestedDuration; }
    public Integer getPriority() { return priority; }
    public void setPriority(Integer priority) { this.priority = priority; }
    public Integer getStatus() { return status; }
    public void setStatus(Integer status) { this.status = status; }
}
