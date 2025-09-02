package com.example.demo.controller;

import com.example.demo.model.dto.ApiResponse;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.sql.DataSource;
import java.sql.Connection;
import java.util.HashMap;
import java.util.Map;

@RestController
public class HealthController {
    
    @Autowired
    private DataSource dataSource;
    
    @Autowired
    private RedisTemplate<String, Object> redisTemplate;
    
    /**
     * Health check endpoint
     */
    @GetMapping("/health")
    public ResponseEntity<ApiResponse<Map<String, Object>>> healthCheck() {
        Map<String, Object> health = new HashMap<>();
        boolean allHealthy = true;
        
        // Check MySQL connection
        try {
            Connection connection = dataSource.getConnection();
            connection.close();
            health.put("mysql", "UP");
        } catch (Exception e) {
            health.put("mysql", "DOWN - " + e.getMessage());
            allHealthy = false;
        }
        
        // Check Redis connection
        try {
            redisTemplate.opsForValue().get("health-check");
            health.put("redis", "UP");
        } catch (Exception e) {
            health.put("redis", "DOWN - " + e.getMessage());
            allHealthy = false;
        }
        
        health.put("application", "UP");
        health.put("status", allHealthy ? "UP" : "DOWN");
        
        if (allHealthy) {
            return ResponseEntity.ok(ApiResponse.success("System healthy", health));
        } else {
            return ResponseEntity.status(503)
                    .body(ApiResponse.error("System unhealthy", health.toString()));
        }
    }
    
    /**
     * Simple ping endpoint
     */
    @GetMapping("/ping")
    public ResponseEntity<ApiResponse<String>> ping() {
        return ResponseEntity.ok(ApiResponse.success("pong"));
    }
}