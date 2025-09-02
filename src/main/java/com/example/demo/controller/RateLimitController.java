package com.example.demo.controller;

import com.example.demo.model.RateLimit;
import com.example.demo.model.dto.ApiResponse;
import com.example.demo.model.dto.CheckResult;
import com.example.demo.model.dto.RateLimitRequest;
import com.example.demo.model.dto.UsageInfo;
import com.example.demo.service.RateLimitService;
import jakarta.validation.Valid;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.web.PageableDefault;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.Optional;

@RestController
@RequestMapping("/api/v1")
public class RateLimitController {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitController.class);
    
    @Autowired
    private RateLimitService rateLimitService;
    
    /**
     * POST /limits - Define Rate Limit
     * Set a request limit for a given API key within a time window
     */
    @PostMapping("/limits")
    public ResponseEntity<ApiResponse<RateLimit>> createRateLimit(
            @Valid @RequestBody RateLimitRequest request) {
        
        logger.info("Creating rate limit for API key: {}", request.getApiKey());
        
        try {
            RateLimit rateLimit = rateLimitService.createRateLimit(request);
            
            String message = rateLimitService.rateLimitExists(request.getApiKey()) 
                    ? "Rate limit updated successfully" 
                    : "Rate limit created successfully";
            
            return ResponseEntity.ok(ApiResponse.success(message, rateLimit));
            
        } catch (Exception e) {
            logger.error("Failed to create rate limit for API key: {}", request.getApiKey(), e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to create rate limit"));
        }
    }
    
    /**
     * GET /check?apiKey=xxx - Check API Access
     * Increment usage counter for the key and check if usage exceeds the limit
     */
    @GetMapping("/check")
    public ResponseEntity<ApiResponse<CheckResult>> checkApiAccess(
            @RequestParam String apiKey) {
        
        logger.debug("Checking API access for key: {}", apiKey);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API key parameter is required"));
        }
        
        try {
            CheckResult result = rateLimitService.checkApiAccess(apiKey.trim());
            
            if (!result.isAllowed()) {
                // Return 429 Too Many Requests for blocked requests
                return ResponseEntity.status(HttpStatus.TOO_MANY_REQUESTS)
                        .body(ApiResponse.error("Request blocked", result.getReason()));
            } else {
                return ResponseEntity.ok(ApiResponse.success("Request allowed", result));
            }
            
        } catch (Exception e) {
            logger.error("Failed to check API access for key: {}", apiKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to check API access"));
        }
    }
    
    /**
     * GET /usage?apiKey=xxx - Query Usage
     * Return current usage count, remaining quota, and window TTL
     */
    @GetMapping("/usage")
    public ResponseEntity<ApiResponse<UsageInfo>> getUsage(
            @RequestParam String apiKey) {
        
        logger.debug("Getting usage info for API key: {}", apiKey);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API key parameter is required"));
        }
        
        try {
            UsageInfo usageInfo = rateLimitService.getUsageInfo(apiKey.trim());
            
            if (usageInfo == null) {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Rate limit not found for API key"));
            }
            
            return ResponseEntity.ok(ApiResponse.success("Usage retrieved successfully", usageInfo));
            
        } catch (Exception e) {
            logger.error("Failed to get usage info for API key: {}", apiKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve usage information"));
        }
    }
    
    /**
     * DELETE /limits/{apiKey} - Remove Limit Rule
     * Remove the rate limit configuration from MySQL and clear Redis entries
     */
    @DeleteMapping("/limits/{apiKey}")
    public ResponseEntity<ApiResponse<Object>> deleteRateLimit(
            @PathVariable String apiKey) {
        
        logger.info("Deleting rate limit for API key: {}", apiKey);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API key path parameter is required"));
        }
        
        try {
            boolean deleted = rateLimitService.deleteRateLimit(apiKey.trim());
            
            if (deleted) {
                return ResponseEntity.ok(ApiResponse.success("Rate limit deleted successfully"));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Rate limit not found for API key"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to delete rate limit for API key: {}", apiKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to delete rate limit"));
        }
    }
    
    /**
     * GET /limits - View All Limits
     * List all active API keys and their associated limits with pagination support
     */
    @GetMapping("/limits")
    public ResponseEntity<ApiResponse<Page<RateLimit>>> getAllRateLimits(
            @PageableDefault(size = 20, sort = "createdAt") Pageable pageable) {
        
        logger.debug("Retrieving all rate limits with pagination: {}", pageable);
        
        try {
            Page<RateLimit> rateLimits = rateLimitService.getAllRateLimits(pageable);
            
            String message = String.format("Retrieved %d rate limits (page %d of %d)", 
                                          rateLimits.getNumberOfElements(),
                                          rateLimits.getNumber() + 1,
                                          rateLimits.getTotalPages());
            
            return ResponseEntity.ok(ApiResponse.success(message, rateLimits));
            
        } catch (Exception e) {
            logger.error("Failed to retrieve rate limits", e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve rate limits"));
        }
    }
    
    /**
     * GET /limits/{apiKey} - Get specific rate limit
     * Retrieve rate limit configuration for a specific API key
     */
    @GetMapping("/limits/{apiKey}")
    public ResponseEntity<ApiResponse<RateLimit>> getRateLimit(
            @PathVariable String apiKey) {
        
        logger.debug("Getting rate limit for API key: {}", apiKey);
        
        if (apiKey == null || apiKey.trim().isEmpty()) {
            return ResponseEntity.badRequest()
                    .body(ApiResponse.error("API key path parameter is required"));
        }
        
        try {
            Optional<RateLimit> rateLimit = rateLimitService.getRateLimitByApiKey(apiKey.trim());
            
            if (rateLimit.isPresent()) {
                return ResponseEntity.ok(ApiResponse.success("Rate limit retrieved successfully", rateLimit.get()));
            } else {
                return ResponseEntity.status(HttpStatus.NOT_FOUND)
                        .body(ApiResponse.error("Rate limit not found for API key"));
            }
            
        } catch (Exception e) {
            logger.error("Failed to get rate limit for API key: {}", apiKey, e);
            return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR)
                    .body(ApiResponse.error("Failed to retrieve rate limit"));
        }
    }
}