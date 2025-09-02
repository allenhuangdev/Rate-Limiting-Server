package com.example.demo.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Service;

import java.time.Duration;

@Service
public class RedisService {
    
    private static final Logger logger = LoggerFactory.getLogger(RedisService.class);
    private static final String RATE_LIMIT_KEY_PREFIX = "rate_limit:";
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Increment counter for API key and set TTL if it's the first request
     * Uses Redis INCR command for atomic increment
     */
    public Long incrementCounter(String apiKey, int windowSeconds) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            // Increment counter atomically
            Long currentCount = redisTemplate.opsForValue().increment(key);
            
            // If this is the first increment (count = 1), set the TTL
            if (currentCount != null && currentCount == 1) {
                redisTemplate.expire(key, Duration.ofSeconds(windowSeconds));
                logger.debug("Set TTL {} seconds for key: {}", windowSeconds, key);
            }
            
            logger.debug("Incremented counter for key: {}, current count: {}", key, currentCount);
            return currentCount != null ? currentCount : 0L;
            
        } catch (Exception e) {
            logger.error("Failed to increment counter for key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }
    
    /**
     * Get current count for API key
     */
    public Long getCurrentCount(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Object count = redisTemplate.opsForValue().get(key);
            Long result = count != null ? Long.parseLong(count.toString()) : 0L;
            logger.debug("Retrieved current count for key: {}, count: {}", key, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to get current count for key: {}", key, e);
            return 0L;
        }
    }
    
    /**
     * Get TTL (Time To Live) for API key in seconds
     */
    public Long getTtl(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Long ttl = redisTemplate.getExpire(key);
            logger.debug("Retrieved TTL for key: {}, ttl: {} seconds", key, ttl);
            return ttl != null ? ttl : -1L;
            
        } catch (Exception e) {
            logger.error("Failed to get TTL for key: {}", key, e);
            return -1L;
        }
    }
    
    /**
     * Delete counter for API key
     */
    public boolean deleteCounter(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Boolean deleted = redisTemplate.delete(key);
            boolean result = deleted != null && deleted;
            logger.debug("Deleted counter for key: {}, success: {}", key, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to delete counter for key: {}", key, e);
            return false;
        }
    }
    
    /**
     * Set counter value with TTL
     */
    public void setCounterWithTtl(String apiKey, long value, int windowSeconds) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            redisTemplate.opsForValue().set(key, value, Duration.ofSeconds(windowSeconds));
            logger.debug("Set counter for key: {}, value: {}, ttl: {} seconds", 
                        key, value, windowSeconds);
            
        } catch (Exception e) {
            logger.error("Failed to set counter with TTL for key: {}", key, e);
            throw new RuntimeException("Redis operation failed", e);
        }
    }
    
    /**
     * Check if key exists in Redis
     */
    public boolean keyExists(String apiKey) {
        String key = RATE_LIMIT_KEY_PREFIX + apiKey;
        
        try {
            Boolean exists = redisTemplate.hasKey(key);
            boolean result = exists != null && exists;
            logger.debug("Checked key existence: {}, exists: {}", key, result);
            return result;
            
        } catch (Exception e) {
            logger.error("Failed to check key existence for key: {}", key, e);
            return false;
        }
    }
}