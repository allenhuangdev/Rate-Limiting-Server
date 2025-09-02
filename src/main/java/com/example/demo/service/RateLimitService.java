package com.example.demo.service;

import com.example.demo.model.RateLimit;
import com.example.demo.model.dto.CheckResult;
import com.example.demo.model.dto.RateLimitRequest;
import com.example.demo.model.dto.UsageInfo;
import com.example.demo.mq.RateLimitEventProducer;
import com.example.demo.repository.RateLimitRepository;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Optional;

@Service
@Transactional
public class RateLimitService {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitService.class);
    
    @Autowired
    private RateLimitRepository rateLimitRepository;
    
    @Autowired
    private RedisService redisService;
    
    @Autowired
    private RateLimitEventProducer eventProducer;
    
    /**
     * Create or update rate limit for API key
     */
    public RateLimit createRateLimit(RateLimitRequest request) {
        logger.info("Creating/updating rate limit for API key: {}", request.getApiKey());
        
        Optional<RateLimit> existingLimit = rateLimitRepository.findByApiKey(request.getApiKey());
        
        RateLimit rateLimit;
        if (existingLimit.isPresent()) {
            // Update existing rate limit
            rateLimit = existingLimit.get();
            rateLimit.setRequestLimit(request.getLimit());
            rateLimit.setWindowSeconds(request.getWindowSeconds());
            logger.info("Updated existing rate limit for API key: {}", request.getApiKey());
        } else {
            // Create new rate limit
            rateLimit = new RateLimit(request.getApiKey(), request.getLimit(), request.getWindowSeconds());
            logger.info("Created new rate limit for API key: {}", request.getApiKey());
        }
        
        RateLimit savedLimit = rateLimitRepository.save(rateLimit);
        
        // Clear any existing Redis counter when rate limit is updated
        redisService.deleteCounter(request.getApiKey());
        logger.debug("Cleared Redis counter for API key: {}", request.getApiKey());
        
        // Send MQ event
        try {
            if (existingLimit.isPresent()) {
                eventProducer.sendLimitUpdatedEvent(request.getApiKey(), request.getLimit(), request.getWindowSeconds());
            } else {
                eventProducer.sendLimitCreatedEvent(request.getApiKey(), request.getLimit(), request.getWindowSeconds());
            }
        } catch (Exception e) {
            logger.warn("Failed to send MQ event for rate limit operation", e);
        }
        
        return savedLimit;
    }
    
    /**
     * Check API access and increment usage counter
     */
    public CheckResult checkApiAccess(String apiKey) {
        logger.debug("Checking API access for key: {}", apiKey);
        
        // Find rate limit configuration
        Optional<RateLimit> rateLimitOpt = rateLimitRepository.findByApiKey(apiKey);
        if (rateLimitOpt.isEmpty()) {
            logger.warn("Rate limit not found for API key: {}", apiKey);
            return CheckResult.notFound(apiKey);
        }
        
        RateLimit rateLimit = rateLimitOpt.get();
        
        // Increment usage counter in Redis
        Long currentUsage = redisService.incrementCounter(apiKey, rateLimit.getWindowSeconds());
        
        // Check if usage exceeds limit
        if (currentUsage > rateLimit.getRequestLimit()) {
            logger.warn("Rate limit exceeded for API key: {}, usage: {}, limit: {}", 
                       apiKey, currentUsage, rateLimit.getRequestLimit());
                       
            // Send MQ event for rate limit exceeded
            try {
                eventProducer.sendLimitExceededEvent(apiKey, currentUsage.intValue(), 
                                                   rateLimit.getRequestLimit(), "unknown");
            } catch (Exception e) {
                logger.warn("Failed to send MQ event for rate limit exceeded", e);
            }
                       
            return CheckResult.blocked(
                apiKey, 
                "Rate limit exceeded", 
                currentUsage.intValue(), 
                rateLimit.getRequestLimit()
            );
        }
        
        // Calculate remaining quota and TTL
        Integer remainingQuota = Math.max(0, rateLimit.getRequestLimit() - currentUsage.intValue());
        Long ttlSeconds = redisService.getTtl(apiKey);
        Integer windowTtl = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds.intValue() : null;
        
        logger.debug("API access allowed for key: {}, usage: {}, remaining: {}", 
                    apiKey, currentUsage, remainingQuota);
        
        return CheckResult.allowed(
            apiKey,
            currentUsage.intValue(),
            remainingQuota,
            windowTtl,
            rateLimit.getRequestLimit()
        );
    }
    
    /**
     * Get current usage information for API key
     */
    public UsageInfo getUsageInfo(String apiKey) {
        logger.debug("Getting usage info for API key: {}", apiKey);
        
        // Find rate limit configuration
        Optional<RateLimit> rateLimitOpt = rateLimitRepository.findByApiKey(apiKey);
        if (rateLimitOpt.isEmpty()) {
            logger.warn("Rate limit not found for API key: {}", apiKey);
            return null;
        }
        
        RateLimit rateLimit = rateLimitOpt.get();
        
        // Get current usage from Redis
        Long currentUsage = redisService.getCurrentCount(apiKey);
        Integer usage = currentUsage != null ? currentUsage.intValue() : 0;
        
        // Calculate remaining quota
        Integer remainingQuota = Math.max(0, rateLimit.getRequestLimit() - usage);
        
        // Get TTL
        Long ttlSeconds = redisService.getTtl(apiKey);
        Integer windowTtl = ttlSeconds != null && ttlSeconds > 0 ? ttlSeconds.intValue() : null;
        
        logger.debug("Retrieved usage info for key: {}, usage: {}, remaining: {}", 
                    apiKey, usage, remainingQuota);
        
        return new UsageInfo(
            apiKey,
            usage,
            remainingQuota,
            windowTtl,
            rateLimit.getRequestLimit(),
            rateLimit.getWindowSeconds()
        );
    }
    
    /**
     * Delete rate limit configuration
     */
    public boolean deleteRateLimit(String apiKey) {
        logger.info("Deleting rate limit for API key: {}", apiKey);
        
        // Delete from database
        int deletedCount = rateLimitRepository.deleteByApiKey(apiKey);
        
        if (deletedCount > 0) {
            // Clear Redis counter
            redisService.deleteCounter(apiKey);
            logger.info("Successfully deleted rate limit for API key: {}", apiKey);
            
            // Send MQ event for rate limit deleted
            try {
                eventProducer.sendLimitDeletedEvent(apiKey);
            } catch (Exception e) {
                logger.warn("Failed to send MQ event for rate limit deletion", e);
            }
            
            return true;
        } else {
            logger.warn("Rate limit not found for deletion, API key: {}", apiKey);
            return false;
        }
    }
    
    /**
     * Get all rate limits with pagination
     */
    @Transactional(readOnly = true)
    public Page<RateLimit> getAllRateLimits(Pageable pageable) {
        logger.debug("Retrieving all rate limits with pagination: {}", pageable);
        return rateLimitRepository.findAll(pageable);
    }
    
    /**
     * Check if rate limit exists for API key
     */
    @Transactional(readOnly = true)
    public boolean rateLimitExists(String apiKey) {
        boolean exists = rateLimitRepository.existsByApiKey(apiKey);
        logger.debug("Rate limit exists for API key {}: {}", apiKey, exists);
        return exists;
    }
    
    /**
     * Get rate limit by API key
     */
    @Transactional(readOnly = true)
    public Optional<RateLimit> getRateLimitByApiKey(String apiKey) {
        logger.debug("Retrieving rate limit for API key: {}", apiKey);
        return rateLimitRepository.findByApiKey(apiKey);
    }
}