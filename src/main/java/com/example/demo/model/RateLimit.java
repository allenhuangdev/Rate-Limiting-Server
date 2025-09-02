package com.example.demo.model;

import jakarta.persistence.*;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.time.LocalDateTime;

@Entity
@Table(name = "rate_limits")
public class RateLimit {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @NotBlank(message = "API key cannot be blank")
    @Size(max = 255, message = "API key must not exceed 255 characters")
    @Column(name = "api_key", unique = true, nullable = false)
    private String apiKey;
    
    @NotNull(message = "Request limit cannot be null")
    @Min(value = 1, message = "Request limit must be positive")
    @Column(name = "request_limit", nullable = false)
    private Integer requestLimit;
    
    @NotNull(message = "Window seconds cannot be null")
    @Min(value = 1, message = "Window seconds must be positive")
    @Column(name = "window_seconds", nullable = false)
    private Integer windowSeconds;
    
    @Column(name = "created_at")
    private LocalDateTime createdAt;
    
    @Column(name = "updated_at")
    private LocalDateTime updatedAt;
    
    public RateLimit() {
    }
    
    public RateLimit(String apiKey, Integer requestLimit, Integer windowSeconds) {
        this.apiKey = apiKey;
        this.requestLimit = requestLimit;
        this.windowSeconds = windowSeconds;
    }
    
    @PrePersist
    protected void onCreate() {
        createdAt = LocalDateTime.now();
        updatedAt = LocalDateTime.now();
    }
    
    @PreUpdate
    protected void onUpdate() {
        updatedAt = LocalDateTime.now();
    }
    
    // Getters and Setters
    public Long getId() {
        return id;
    }
    
    public void setId(Long id) {
        this.id = id;
    }
    
    public String getApiKey() {
        return apiKey;
    }
    
    public void setApiKey(String apiKey) {
        this.apiKey = apiKey;
    }
    
    public Integer getRequestLimit() {
        return requestLimit;
    }
    
    public void setRequestLimit(Integer requestLimit) {
        this.requestLimit = requestLimit;
    }
    
    public Integer getWindowSeconds() {
        return windowSeconds;
    }
    
    public void setWindowSeconds(Integer windowSeconds) {
        this.windowSeconds = windowSeconds;
    }
    
    public LocalDateTime getCreatedAt() {
        return createdAt;
    }
    
    public void setCreatedAt(LocalDateTime createdAt) {
        this.createdAt = createdAt;
    }
    
    public LocalDateTime getUpdatedAt() {
        return updatedAt;
    }
    
    public void setUpdatedAt(LocalDateTime updatedAt) {
        this.updatedAt = updatedAt;
    }
    
    @Override
    public String toString() {
        return "RateLimit{" +
                "id=" + id +
                ", apiKey='" + apiKey + '\'' +
                ", requestLimit=" + requestLimit +
                ", windowSeconds=" + windowSeconds +
                ", createdAt=" + createdAt +
                ", updatedAt=" + updatedAt +
                '}';
    }
}