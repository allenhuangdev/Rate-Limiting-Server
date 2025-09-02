package com.example.demo.model.dto;

public class UsageInfo {
    
    private String apiKey;
    private Integer currentUsage;
    private Integer remainingQuota;
    private Integer windowTtl;
    private Integer totalLimit;
    private Integer windowSeconds;
    
    public UsageInfo() {
    }
    
    public UsageInfo(String apiKey, Integer currentUsage, Integer remainingQuota, 
                    Integer windowTtl, Integer totalLimit, Integer windowSeconds) {
        this.apiKey = apiKey;
        this.currentUsage = currentUsage;
        this.remainingQuota = remainingQuota;
        this.windowTtl = windowTtl;
        this.totalLimit = totalLimit;
        this.windowSeconds = windowSeconds;
    }
    
    // Getters and Setters
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public Integer getCurrentUsage() {
        return currentUsage;
    }
    
    public void setCurrentUsage(Integer currentUsage) {
        this.currentUsage = currentUsage;
    }
    
    public Integer getRemainingQuota() {
        return remainingQuota;
    }
    
    public void setRemainingQuota(Integer remainingQuota) {
        this.remainingQuota = remainingQuota;
    }
    
    public Integer getWindowTtl() {
        return windowTtl;
    }
    
    public void setWindowTtl(Integer windowTtl) {
        this.windowTtl = windowTtl;
    }
    
    public Integer getTotalLimit() {
        return totalLimit;
    }
    
    public void setTotalLimit(Integer totalLimit) {
        this.totalLimit = totalLimit;
    }
    
    public Integer getWindowSeconds() {
        return windowSeconds;
    }
    
    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
    
    @Override
    public String toString() {
        return "UsageInfo{" +
                "apiKey='" + apiKey + '\'' +
                ", currentUsage=" + currentUsage +
                ", remainingQuota=" + remainingQuota +
                ", windowTtl=" + windowTtl +
                ", totalLimit=" + totalLimit +
                ", windowSeconds=" + windowSeconds +
                '}';
    }
}