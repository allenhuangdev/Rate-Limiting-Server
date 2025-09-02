package com.example.demo.model.dto;

public class CheckResult {
    
    private String apiKey;
    private boolean allowed;
    private String reason;
    private Integer currentUsage;
    private Integer remainingQuota;
    private Integer windowTtl;
    private Integer totalLimit;
    
    public CheckResult() {
    }
    
    public CheckResult(String apiKey, boolean allowed, String reason) {
        this.apiKey = apiKey;
        this.allowed = allowed;
        this.reason = reason;
    }
    
    public CheckResult(String apiKey, boolean allowed, String reason, 
                      Integer currentUsage, Integer remainingQuota, 
                      Integer windowTtl, Integer totalLimit) {
        this(apiKey, allowed, reason);
        this.currentUsage = currentUsage;
        this.remainingQuota = remainingQuota;
        this.windowTtl = windowTtl;
        this.totalLimit = totalLimit;
    }
    
    // Static factory methods
    public static CheckResult allowed(String apiKey, Integer currentUsage, 
                                    Integer remainingQuota, Integer windowTtl, 
                                    Integer totalLimit) {
        return new CheckResult(apiKey, true, "Request allowed", 
                             currentUsage, remainingQuota, windowTtl, totalLimit);
    }
    
    public static CheckResult blocked(String apiKey, String reason, 
                                    Integer currentUsage, Integer totalLimit) {
        return new CheckResult(apiKey, false, reason, currentUsage, 0, null, totalLimit);
    }
    
    public static CheckResult notFound(String apiKey) {
        return new CheckResult(apiKey, false, "API key not found");
    }
    
    // Getters and Setters
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public boolean isAllowed() {
        return allowed;
    }
    
    public void setAllowed(boolean allowed) {
        this.allowed = allowed;
    }
    
    public String getReason() {
        return reason;
    }
    
    public void setReason(String reason) {
        this.reason = reason;
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
    
    @Override
    public String toString() {
        return "CheckResult{" +
                "apiKey='" + apiKey + '\'' +
                ", allowed=" + allowed +
                ", reason='" + reason + '\'' +
                ", currentUsage=" + currentUsage +
                ", remainingQuota=" + remainingQuota +
                ", windowTtl=" + windowTtl +
                ", totalLimit=" + totalLimit +
                '}';
    }
}