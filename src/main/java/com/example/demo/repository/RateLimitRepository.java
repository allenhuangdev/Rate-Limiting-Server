package com.example.demo.repository;

import com.example.demo.model.RateLimit;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface RateLimitRepository extends JpaRepository<RateLimit, Long> {
    
    /**
     * Find rate limit configuration by API key
     */
    Optional<RateLimit> findByApiKey(String apiKey);
    
    /**
     * Check if rate limit exists for API key
     */
    boolean existsByApiKey(String apiKey);
    
    /**
     * Delete rate limit by API key
     */
    @Modifying
    @Query("DELETE FROM RateLimit r WHERE r.apiKey = :apiKey")
    int deleteByApiKey(@Param("apiKey") String apiKey);
    
    /**
     * Find all rate limits with pagination
     */
    Page<RateLimit> findAll(Pageable pageable);
    
    /**
     * Find rate limits by request limit range
     */
    @Query("SELECT r FROM RateLimit r WHERE r.requestLimit BETWEEN :minLimit AND :maxLimit")
    Page<RateLimit> findByRequestLimitBetween(@Param("minLimit") Integer minLimit, 
                                             @Param("maxLimit") Integer maxLimit, 
                                             Pageable pageable);
    
    /**
     * Find rate limits by window seconds range
     */
    @Query("SELECT r FROM RateLimit r WHERE r.windowSeconds BETWEEN :minWindow AND :maxWindow")
    Page<RateLimit> findByWindowSecondsBetween(@Param("minWindow") Integer minWindow, 
                                              @Param("maxWindow") Integer maxWindow, 
                                              Pageable pageable);
    
    /**
     * Count total rate limits
     */
    @Query("SELECT COUNT(r) FROM RateLimit r")
    long countRateLimits();
}