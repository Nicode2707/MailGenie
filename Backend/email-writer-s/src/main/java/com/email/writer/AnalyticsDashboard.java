package com.email.writer;

import java.util.Map;

public class AnalyticsDashboard {
    private long totalSent;
    private long totalDelivered;
    private long totalOpened;
    private long totalClicked;
    private long totalBounced;
    private long totalComplaints;
    private long totalUnsubscribed;
    private double openRate;
    private double clickRate;
    private double bounceRate;
    private double complaintRate;
    private double unsubscribeRate;
    private double deliveryRate;
    private double clickToOpenRate;
    private Double avgTimeToOpenSeconds;
    private Double avgTimeToClickSeconds;
    private Map<String, Long> opensByDevice;
    private Map<String, Long> opensByClient;
    private Map<String, Long> engagementByCountry;
    private Map<String, Long> eventsByType;
    private Map<String, Long> dailyTrend;
    private long hotRecipients;
    private long warmRecipients;
    private long coldRecipients;
    private long inactiveRecipients;
    private long atRiskRecipients;
    private long vipRecipients;
    private double avgEngagementScore;
    private long totalSegments;
    private long totalRecipients;

    public AnalyticsDashboard() {}

    public long getTotalSent() { return totalSent; }
    public void setTotalSent(long totalSent) { this.totalSent = totalSent; }
    public long getTotalDelivered() { return totalDelivered; }
    public void setTotalDelivered(long totalDelivered) { this.totalDelivered = totalDelivered; }
    public long getTotalOpened() { return totalOpened; }
    public void setTotalOpened(long totalOpened) { this.totalOpened = totalOpened; }
    public long getTotalClicked() { return totalClicked; }
    public void setTotalClicked(long totalClicked) { this.totalClicked = totalClicked; }
    public long getTotalBounced() { return totalBounced; }
    public void setTotalBounced(long totalBounced) { this.totalBounced = totalBounced; }
    public long getTotalComplaints() { return totalComplaints; }
    public void setTotalComplaints(long totalComplaints) { this.totalComplaints = totalComplaints; }
    public long getTotalUnsubscribed() { return totalUnsubscribed; }
    public void setTotalUnsubscribed(long totalUnsubscribed) { this.totalUnsubscribed = totalUnsubscribed; }
    public double getOpenRate() { return openRate; }
    public void setOpenRate(double openRate) { this.openRate = openRate; }
    public double getClickRate() { return clickRate; }
    public void setClickRate(double clickRate) { this.clickRate = clickRate; }
    public double getBounceRate() { return bounceRate; }
    public void setBounceRate(double bounceRate) { this.bounceRate = bounceRate; }
    public double getComplaintRate() { return complaintRate; }
    public void setComplaintRate(double complaintRate) { this.complaintRate = complaintRate; }
    public double getUnsubscribeRate() { return unsubscribeRate; }
    public void setUnsubscribeRate(double unsubscribeRate) { this.unsubscribeRate = unsubscribeRate; }
    public double getDeliveryRate() { return deliveryRate; }
    public void setDeliveryRate(double deliveryRate) { this.deliveryRate = deliveryRate; }
    public double getClickToOpenRate() { return clickToOpenRate; }
    public void setClickToOpenRate(double clickToOpenRate) { this.clickToOpenRate = clickToOpenRate; }
    public Double getAvgTimeToOpenSeconds() { return avgTimeToOpenSeconds; }
    public void setAvgTimeToOpenSeconds(Double avgTimeToOpenSeconds) { this.avgTimeToOpenSeconds = avgTimeToOpenSeconds; }
    public Double getAvgTimeToClickSeconds() { return avgTimeToClickSeconds; }
    public void setAvgTimeToClickSeconds(Double avgTimeToClickSeconds) { this.avgTimeToClickSeconds = avgTimeToClickSeconds; }
    public Map<String, Long> getOpensByDevice() { return opensByDevice; }
    public void setOpensByDevice(Map<String, Long> opensByDevice) { this.opensByDevice = opensByDevice; }
    public Map<String, Long> getOpensByClient() { return opensByClient; }
    public void setOpensByClient(Map<String, Long> opensByClient) { this.opensByClient = opensByClient; }
    public Map<String, Long> getEngagementByCountry() { return engagementByCountry; }
    public void setEngagementByCountry(Map<String, Long> engagementByCountry) { this.engagementByCountry = engagementByCountry; }
    public Map<String, Long> getEventsByType() { return eventsByType; }
    public void setEventsByType(Map<String, Long> eventsByType) { this.eventsByType = eventsByType; }
    public Map<String, Long> getDailyTrend() { return dailyTrend; }
    public void setDailyTrend(Map<String, Long> dailyTrend) { this.dailyTrend = dailyTrend; }
    public long getHotRecipients() { return hotRecipients; }
    public void setHotRecipients(long hotRecipients) { this.hotRecipients = hotRecipients; }
    public long getWarmRecipients() { return warmRecipients; }
    public void setWarmRecipients(long warmRecipients) { this.warmRecipients = warmRecipients; }
    public long getColdRecipients() { return coldRecipients; }
    public void setColdRecipients(long coldRecipients) { this.coldRecipients = coldRecipients; }
    public long getInactiveRecipients() { return inactiveRecipients; }
    public void setInactiveRecipients(long inactiveRecipients) { this.inactiveRecipients = inactiveRecipients; }
    public long getAtRiskRecipients() { return atRiskRecipients; }
    public void setAtRiskRecipients(long atRiskRecipients) { this.atRiskRecipients = atRiskRecipients; }
    public long getVipRecipients() { return vipRecipients; }
    public void setVipRecipients(long vipRecipients) { this.vipRecipients = vipRecipients; }
    public double getAvgEngagementScore() { return avgEngagementScore; }
    public void setAvgEngagementScore(double avgEngagementScore) { this.avgEngagementScore = avgEngagementScore; }
    public long getTotalSegments() { return totalSegments; }
    public void setTotalSegments(long totalSegments) { this.totalSegments = totalSegments; }
    public long getTotalRecipients() { return totalRecipients; }
    public void setTotalRecipients(long totalRecipients) { this.totalRecipients = totalRecipients; }
}
