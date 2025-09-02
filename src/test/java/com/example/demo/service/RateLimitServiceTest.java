package com.example.demo.service;

import com.example.demo.model.RateLimit;
import com.example.demo.model.dto.CheckResult;
import com.example.demo.model.dto.RateLimitRequest;
import com.example.demo.model.dto.UsageInfo;
import com.example.demo.mq.RateLimitEventProducer;
import com.example.demo.repository.RateLimitRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;

import java.util.Arrays;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.*;
import static org.mockito.Mockito.*;

@ExtendWith(MockitoExtension.class)
class RateLimitServiceTest {
    
    @Mock
    private RateLimitRepository rateLimitRepository;
    
    @Mock
    private RedisService redisService;
    
    @Mock
    private RateLimitEventProducer eventProducer;
    
    @InjectMocks
    private RateLimitService rateLimitService;
    
    private RateLimit testRateLimit;
    private RateLimitRequest testRequest;
    
    @BeforeEach
    void setUp() {
        testRateLimit = new RateLimit("test-key", 100, 60);
        testRateLimit.setId(1L);
        
        testRequest = new RateLimitRequest("test-key", 100, 60);
    }
    
    @Test
    void testCreateRateLimit_NewLimit() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.empty());
        when(rateLimitRepository.save(any(RateLimit.class))).thenReturn(testRateLimit);
        when(redisService.deleteCounter("test-key")).thenReturn(true);
        
        // Act
        RateLimit result = rateLimitService.createRateLimit(testRequest);
        
        // Assert
        assertNotNull(result);
        assertEquals("test-key", result.getApiKey());
        assertEquals(100, result.getRequestLimit());
        assertEquals(60, result.getWindowSeconds());
        
        verify(rateLimitRepository).findByApiKey("test-key");
        verify(rateLimitRepository).save(any(RateLimit.class));
        verify(redisService).deleteCounter("test-key");
        verify(eventProducer).sendLimitCreatedEvent("test-key", 100, 60);
    }
    
    @Test
    void testCreateRateLimit_UpdateExisting() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        when(rateLimitRepository.save(any(RateLimit.class))).thenReturn(testRateLimit);
        when(redisService.deleteCounter("test-key")).thenReturn(true);
        
        // Act
        RateLimit result = rateLimitService.createRateLimit(testRequest);
        
        // Assert
        assertNotNull(result);
        verify(eventProducer).sendLimitUpdatedEvent("test-key", 100, 60);
    }
    
    @Test
    void testCheckApiAccess_NotFound() {
        // Arrange
        when(rateLimitRepository.findByApiKey("unknown-key")).thenReturn(Optional.empty());
        
        // Act
        CheckResult result = rateLimitService.checkApiAccess("unknown-key");
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("API key not found", result.getReason());
        assertEquals("unknown-key", result.getApiKey());
    }
    
    @Test
    void testCheckApiAccess_AllowedRequest() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        when(redisService.incrementCounter("test-key", 60)).thenReturn(5L);
        when(redisService.getTtl("test-key")).thenReturn(50L);
        
        // Act
        CheckResult result = rateLimitService.checkApiAccess("test-key");
        
        // Assert
        assertNotNull(result);
        assertTrue(result.isAllowed());
        assertEquals("Request allowed", result.getReason());
        assertEquals(5, result.getCurrentUsage());
        assertEquals(95, result.getRemainingQuota());
        assertEquals(50, result.getWindowTtl());
        assertEquals(100, result.getTotalLimit());
    }
    
    @Test
    void testCheckApiAccess_BlockedRequest() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        when(redisService.incrementCounter("test-key", 60)).thenReturn(101L);
        
        // Act
        CheckResult result = rateLimitService.checkApiAccess("test-key");
        
        // Assert
        assertNotNull(result);
        assertFalse(result.isAllowed());
        assertEquals("Rate limit exceeded", result.getReason());
        assertEquals(101, result.getCurrentUsage());
        assertEquals(0, result.getRemainingQuota());
        assertEquals(100, result.getTotalLimit());
        
        verify(eventProducer).sendLimitExceededEvent("test-key", 101, 100, "unknown");
    }
    
    @Test
    void testGetUsageInfo_Success() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        when(redisService.getCurrentCount("test-key")).thenReturn(25L);
        when(redisService.getTtl("test-key")).thenReturn(30L);
        
        // Act
        UsageInfo result = rateLimitService.getUsageInfo("test-key");
        
        // Assert
        assertNotNull(result);
        assertEquals("test-key", result.getApiKey());
        assertEquals(25, result.getCurrentUsage());
        assertEquals(75, result.getRemainingQuota());
        assertEquals(30, result.getWindowTtl());
        assertEquals(100, result.getTotalLimit());
        assertEquals(60, result.getWindowSeconds());
    }
    
    @Test
    void testGetUsageInfo_NotFound() {
        // Arrange
        when(rateLimitRepository.findByApiKey("unknown-key")).thenReturn(Optional.empty());
        
        // Act
        UsageInfo result = rateLimitService.getUsageInfo("unknown-key");
        
        // Assert
        assertNull(result);
    }
    
    @Test
    void testDeleteRateLimit_Success() {
        // Arrange
        when(rateLimitRepository.deleteByApiKey("test-key")).thenReturn(1);
        when(redisService.deleteCounter("test-key")).thenReturn(true);
        
        // Act
        boolean result = rateLimitService.deleteRateLimit("test-key");
        
        // Assert
        assertTrue(result);
        verify(rateLimitRepository).deleteByApiKey("test-key");
        verify(redisService).deleteCounter("test-key");
        verify(eventProducer).sendLimitDeletedEvent("test-key");
    }
    
    @Test
    void testDeleteRateLimit_NotFound() {
        // Arrange
        when(rateLimitRepository.deleteByApiKey("unknown-key")).thenReturn(0);
        
        // Act
        boolean result = rateLimitService.deleteRateLimit("unknown-key");
        
        // Assert
        assertFalse(result);
        verify(rateLimitRepository).deleteByApiKey("unknown-key");
        verify(redisService, never()).deleteCounter(anyString());
        verify(eventProducer, never()).sendLimitDeletedEvent(anyString());
    }
    
    @Test
    void testGetAllRateLimits() {
        // Arrange
        Pageable pageable = PageRequest.of(0, 10);
        Page<RateLimit> expectedPage = new PageImpl<>(Arrays.asList(testRateLimit));
        when(rateLimitRepository.findAll(pageable)).thenReturn(expectedPage);
        
        // Act
        Page<RateLimit> result = rateLimitService.getAllRateLimits(pageable);
        
        // Assert
        assertNotNull(result);
        assertEquals(1, result.getContent().size());
        assertEquals("test-key", result.getContent().get(0).getApiKey());
    }
    
    @Test
    void testRateLimitExists() {
        // Arrange
        when(rateLimitRepository.existsByApiKey("test-key")).thenReturn(true);
        when(rateLimitRepository.existsByApiKey("unknown-key")).thenReturn(false);
        
        // Act & Assert
        assertTrue(rateLimitService.rateLimitExists("test-key"));
        assertFalse(rateLimitService.rateLimitExists("unknown-key"));
    }
    
    @Test
    void testGetRateLimitByApiKey() {
        // Arrange
        when(rateLimitRepository.findByApiKey("test-key")).thenReturn(Optional.of(testRateLimit));
        when(rateLimitRepository.findByApiKey("unknown-key")).thenReturn(Optional.empty());
        
        // Act & Assert
        Optional<RateLimit> result1 = rateLimitService.getRateLimitByApiKey("test-key");
        Optional<RateLimit> result2 = rateLimitService.getRateLimitByApiKey("unknown-key");
        
        assertTrue(result1.isPresent());
        assertEquals("test-key", result1.get().getApiKey());
        
        assertFalse(result2.isPresent());
    }
}