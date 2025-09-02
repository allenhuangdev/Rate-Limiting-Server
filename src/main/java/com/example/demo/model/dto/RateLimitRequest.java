package com.example.demo.model.dto;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

public class RateLimitRequest {
    
    @NotBlank(message = "API key cannot be blank")
    @Size(max = 255, message = "API key must not exceed 255 characters")
    private String apiKey;
    
    @NotNull(message = "Limit cannot be null")
    @Min(value = 1, message = "Limit must be positive")
    private Integer limit;
    
    @NotNull(message = "Window seconds cannot be null")
    @Min(value = 1, message = "Window seconds must be positive")
    private Integer windowSeconds;
    
    public RateLimitRequest() {
    }
    
    public RateLimitRequest(String apiKey, Integer limit, Integer windowSeconds) {
        this.apiKey = apiKey;
        this.limit = limit;
        this.windowSeconds = windowSeconds;
    }
    
    // Getters and Setters
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public Integer getLimit() {
        return limit;
    }
    
    public void setLimit(Integer limit) {
        this.limit = limit;
    }
    
    public Integer getWindowSeconds() {
        return windowSeconds;
    }
    
    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
    
    @Override
    public String toString() {
        return "RateLimitRequest{" +
                "apiKey='" + apiKey + '\'' +
                ", limit=" + limit +
                ", windowSeconds=" + windowSeconds +
                '}';
    }
}