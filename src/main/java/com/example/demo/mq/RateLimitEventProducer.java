package com.example.demo.mq;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.apache.rocketmq.client.exception.MQBrokerException;
import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.apache.rocketmq.client.producer.SendResult;
import org.apache.rocketmq.common.message.Message;
import org.apache.rocketmq.remoting.exception.RemotingException;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;

@Component
public class RateLimitEventProducer {
    
    private static final Logger logger = LoggerFactory.getLogger(RateLimitEventProducer.class);
    
    private static final String TOPIC_RATE_LIMIT_EVENTS = "rate-limit-events";
    private static final String TAG_LIMIT_EXCEEDED = "LIMIT_EXCEEDED";
    private static final String TAG_LIMIT_CREATED = "LIMIT_CREATED";
    private static final String TAG_LIMIT_UPDATED = "LIMIT_UPDATED";
    private static final String TAG_LIMIT_DELETED = "LIMIT_DELETED";
    
    @Autowired
    private DefaultMQProducer producer;
    
    private final ObjectMapper objectMapper = new ObjectMapper();
    
    /**
     * Send rate limit exceeded event
     */
    public void sendLimitExceededEvent(String apiKey, int currentUsage, int limit, String clientIp) {
        RateLimitExceededEvent event = new RateLimitExceededEvent(
            apiKey, currentUsage, limit, clientIp, System.currentTimeMillis()
        );
        
        sendMessage(TAG_LIMIT_EXCEEDED, event, "Rate limit exceeded for API key: " + apiKey);
    }
    
    /**
     * Send rate limit configuration created event
     */
    public void sendLimitCreatedEvent(String apiKey, int limit, int windowSeconds) {
        RateLimitConfigEvent event = new RateLimitConfigEvent(
            apiKey, limit, windowSeconds, "CREATED", System.currentTimeMillis()
        );
        
        sendMessage(TAG_LIMIT_CREATED, event, "Rate limit created for API key: " + apiKey);
    }
    
    /**
     * Send rate limit configuration updated event
     */
    public void sendLimitUpdatedEvent(String apiKey, int limit, int windowSeconds) {
        RateLimitConfigEvent event = new RateLimitConfigEvent(
            apiKey, limit, windowSeconds, "UPDATED", System.currentTimeMillis()
        );
        
        sendMessage(TAG_LIMIT_UPDATED, event, "Rate limit updated for API key: " + apiKey);
    }
    
    /**
     * Send rate limit configuration deleted event
     */
    public void sendLimitDeletedEvent(String apiKey) {
        RateLimitConfigEvent event = new RateLimitConfigEvent(
            apiKey, 0, 0, "DELETED", System.currentTimeMillis()
        );
        
        sendMessage(TAG_LIMIT_DELETED, event, "Rate limit deleted for API key: " + apiKey);
    }
    
    /**
     * Generic method to send message to RocketMQ
     */
    private void sendMessage(String tag, Object event, String logMessage) {
        try {
            String messageBody = objectMapper.writeValueAsString(event);
            
            Message message = new Message(
                TOPIC_RATE_LIMIT_EVENTS,
                tag,
                messageBody.getBytes("UTF-8")
            );
            
            SendResult sendResult = producer.send(message);
            
            logger.info("{} - MessageId: {}, Status: {}", 
                       logMessage, sendResult.getMsgId(), sendResult.getSendStatus());
                       
        } catch (JsonProcessingException e) {
            logger.error("Failed to serialize event: {}", logMessage, e);
        } catch (MQClientException | RemotingException | MQBrokerException | InterruptedException e) {
            logger.error("Failed to send message: {}", logMessage, e);
        } catch (Exception e) {
            logger.error("Unexpected error while sending message: {}", logMessage, e);
        }
    }
    
    /**
     * Rate limit exceeded event data class
     */
    public static class RateLimitExceededEvent {
        private String apiKey;
        private int currentUsage;
        private int limit;
        private String clientIp;
        private long timestamp;
        
        public RateLimitExceededEvent() {}
        
        public RateLimitExceededEvent(String apiKey, int currentUsage, int limit, String clientIp, long timestamp) {
            this.apiKey = apiKey;
            this.currentUsage = currentUsage;
            this.limit = limit;
            this.clientIp = clientIp;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public int getCurrentUsage() { return currentUsage; }
        public void setCurrentUsage(int currentUsage) { this.currentUsage = currentUsage; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public String getClientIp() { return clientIp; }
        public void setClientIp(String clientIp) { this.clientIp = clientIp; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
    
    /**
     * Rate limit configuration event data class
     */
    public static class RateLimitConfigEvent {
        private String apiKey;
        private int limit;
        private int windowSeconds;
        private String action;
        private long timestamp;
        
        public RateLimitConfigEvent() {}
        
        public RateLimitConfigEvent(String apiKey, int limit, int windowSeconds, String action, long timestamp) {
            this.apiKey = apiKey;
            this.limit = limit;
            this.windowSeconds = windowSeconds;
            this.action = action;
            this.timestamp = timestamp;
        }
        
        // Getters and setters
        public String getApiKey() { return apiKey; }
        public void setApiKey(String apiKey) { this.apiKey = apiKey; }
        public int getLimit() { return limit; }
        public void setLimit(int limit) { this.limit = limit; }
        public int getWindowSeconds() { return windowSeconds; }
        public void setWindowSeconds(int windowSeconds) { this.windowSeconds = windowSeconds; }
        public String getAction() { return action; }
        public void setAction(String action) { this.action = action; }
        public long getTimestamp() { return timestamp; }
        public void setTimestamp(long timestamp) { this.timestamp = timestamp; }
    }
}