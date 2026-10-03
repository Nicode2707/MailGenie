package com.email.writer;

public class SegmentRequest {
    private String userId;
    private String name;
    private String description;
    private String segmentType;
    private String criteriaJson;
    private String triggerEvent;
    private Integer triggerWindowDays;
    private Integer triggerMinCount;

    public SegmentRequest() {}

    public String getUserId() { return userId; }
    public void setUserId(String userId) { this.userId = userId; }
    public String getName() { return name; }
    public void setName(String name) { this.name = name; }
    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }
    public String getSegmentType() { return segmentType; }
    public void setSegmentType(String segmentType) { this.segmentType = segmentType; }
    public String getCriteriaJson() { return criteriaJson; }
    public void setCriteriaJson(String criteriaJson) { this.criteriaJson = criteriaJson; }
    public String getTriggerEvent() { return triggerEvent; }
    public void setTriggerEvent(String triggerEvent) { this.triggerEvent = triggerEvent; }
    public Integer getTriggerWindowDays() { return triggerWindowDays; }
    public void setTriggerWindowDays(Integer triggerWindowDays) { this.triggerWindowDays = triggerWindowDays; }
    public Integer getTriggerMinCount() { return triggerMinCount; }
    public void setTriggerMinCount(Integer triggerMinCount) { this.triggerMinCount = triggerMinCount; }
}
