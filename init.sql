-- Rate Limiting Service Database Schema
-- This script creates the necessary tables for the rate limiting service

-- Create rate_limits table to store API key configurations
CREATE TABLE IF NOT EXISTS rate_limits (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    api_key VARCHAR(255) NOT NULL UNIQUE,
    request_limit INT NOT NULL,
    window_seconds INT NOT NULL,
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    updated_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
    
    INDEX idx_api_key (api_key),
    CONSTRAINT chk_request_limit_positive CHECK (request_limit > 0),
    CONSTRAINT chk_window_seconds_positive CHECK (window_seconds > 0)
);

-- Insert some sample data for testing
INSERT INTO rate_limits (api_key, request_limit, window_seconds) VALUES
('test-key-1', 100, 60),
('test-key-2', 50, 30),
('demo-api-key', 200, 120)
ON DUPLICATE KEY UPDATE 
    request_limit = VALUES(request_limit),
    window_seconds = VALUES(window_seconds),
    updated_at = CURRENT_TIMESTAMP;

-- Display initial data
SELECT 'Rate limits table created successfully with sample data:' AS message;
SELECT * FROM rate_limits;