package com.example.demo.controller;

import com.example.demo.model.RateLimit;
import com.example.demo.model.dto.CheckResult;
import com.example.demo.model.dto.RateLimitRequest;
import com.example.demo.model.dto.UsageInfo;
import com.example.demo.service.RateLimitService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Arrays;
import java.util.Optional;

import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.*;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.*;

@WebMvcTest(RateLimitController.class)
class RateLimitControllerTest {
    
    @Autowired
    private MockMvc mockMvc;
    
    @MockBean
    private RateLimitService rateLimitService;
    
    @Autowired
    private ObjectMapper objectMapper;
    
    private RateLimit testRateLimit;
    private RateLimitRequest testRequest;
    
    @BeforeEach
    void setUp() {
        testRateLimit = new RateLimit("test-key", 100, 60);
        testRateLimit.setId(1L);
        
        testRequest = new RateLimitRequest("test-key", 100, 60);
    }
    
    @Test
    void testCreateRateLimit_Success() throws Exception {
        // Arrange
        when(rateLimitService.createRateLimit(any(RateLimitRequest.class))).thenReturn(testRateLimit);
        when(rateLimitService.rateLimitExists("test-key")).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(testRequest)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rate limit created successfully"))
                .andExpect(jsonPath("$.data.apiKey").value("test-key"))
                .andExpect(jsonPath("$.data.requestLimit").value(100))
                .andExpect(jsonPath("$.data.windowSeconds").value(60));
        
        verify(rateLimitService).createRateLimit(any(RateLimitRequest.class));
    }
    
    @Test
    void testCreateRateLimit_ValidationError() throws Exception {
        // Arrange
        RateLimitRequest invalidRequest = new RateLimitRequest("", -1, 0);
        
        // Act & Assert
        mockMvc.perform(post("/api/v1/limits")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(invalidRequest)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false));
        
        verify(rateLimitService, never()).createRateLimit(any());
    }
    
    @Test
    void testCheckApiAccess_Allowed() throws Exception {
        // Arrange
        CheckResult allowedResult = CheckResult.allowed("test-key", 5, 95, 50, 100);
        when(rateLimitService.checkApiAccess("test-key")).thenReturn(allowedResult);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/check")
                .param("apiKey", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Request allowed"))
                .andExpect(jsonPath("$.data.allowed").value(true))
                .andExpect(jsonPath("$.data.currentUsage").value(5))
                .andExpect(jsonPath("$.data.remainingQuota").value(95));
        
        verify(rateLimitService).checkApiAccess("test-key");
    }
    
    @Test
    void testCheckApiAccess_Blocked() throws Exception {
        // Arrange
        CheckResult blockedResult = CheckResult.blocked("test-key", "Rate limit exceeded", 101, 100);
        when(rateLimitService.checkApiAccess("test-key")).thenReturn(blockedResult);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/check")
                .param("apiKey", "test-key"))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Rate limit exceeded"));
        
        verify(rateLimitService).checkApiAccess("test-key");
    }
    
    @Test
    void testCheckApiAccess_MissingApiKey() throws Exception {
        // Act & Assert
        mockMvc.perform(get("/api/v1/check"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("API key parameter is required"));
        
        verify(rateLimitService, never()).checkApiAccess(any());
    }
    
    @Test
    void testGetUsage_Success() throws Exception {
        // Arrange
        UsageInfo usageInfo = new UsageInfo("test-key", 25, 75, 30, 100, 60);
        when(rateLimitService.getUsageInfo("test-key")).thenReturn(usageInfo);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/usage")
                .param("apiKey", "test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.currentUsage").value(25))
                .andExpect(jsonPath("$.data.remainingQuota").value(75))
                .andExpect(jsonPath("$.data.windowTtl").value(30));
        
        verify(rateLimitService).getUsageInfo("test-key");
    }
    
    @Test
    void testGetUsage_NotFound() throws Exception {
        // Arrange
        when(rateLimitService.getUsageInfo("unknown-key")).thenReturn(null);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/usage")
                .param("apiKey", "unknown-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Rate limit not found for API key"));
        
        verify(rateLimitService).getUsageInfo("unknown-key");
    }
    
    @Test
    void testDeleteRateLimit_Success() throws Exception {
        // Arrange
        when(rateLimitService.deleteRateLimit("test-key")).thenReturn(true);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/limits/test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.message").value("Rate limit deleted successfully"));
        
        verify(rateLimitService).deleteRateLimit("test-key");
    }
    
    @Test
    void testDeleteRateLimit_NotFound() throws Exception {
        // Arrange
        when(rateLimitService.deleteRateLimit("unknown-key")).thenReturn(false);
        
        // Act & Assert
        mockMvc.perform(delete("/api/v1/limits/unknown-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Rate limit not found for API key"));
        
        verify(rateLimitService).deleteRateLimit("unknown-key");
    }
    
    @Test
    void testGetAllRateLimits() throws Exception {
        // Arrange
        Page<RateLimit> page = new PageImpl<>(Arrays.asList(testRateLimit), PageRequest.of(0, 20), 1);
        when(rateLimitService.getAllRateLimits(any())).thenReturn(page);
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/limits"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.content").isArray())
                .andExpect(jsonPath("$.data.content[0].apiKey").value("test-key"))
                .andExpect(jsonPath("$.data.totalElements").value(1));
        
        verify(rateLimitService).getAllRateLimits(any());
    }
    
    @Test
    void testGetRateLimit_Success() throws Exception {
        // Arrange
        when(rateLimitService.getRateLimitByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/limits/test-key"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.success").value(true))
                .andExpect(jsonPath("$.data.apiKey").value("test-key"))
                .andExpect(jsonPath("$.data.requestLimit").value(100));
        
        verify(rateLimitService).getRateLimitByApiKey("test-key");
    }
    
    @Test
    void testGetRateLimit_NotFound() throws Exception {
        // Arrange
        when(rateLimitService.getRateLimitByApiKey("unknown-key")).thenReturn(Optional.empty());
        
        // Act & Assert
        mockMvc.perform(get("/api/v1/limits/unknown-key"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.success").value(false))
                .andExpect(jsonPath("$.error").value("Rate limit not found for API key"));
        
        verify(rateLimitService).getRateLimitByApiKey("unknown-key");
    }
}