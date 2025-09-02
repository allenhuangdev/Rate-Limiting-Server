package com.example.demo.config;

import org.apache.rocketmq.client.exception.MQClientException;
import org.apache.rocketmq.client.producer.DefaultMQProducer;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class RocketMQConfig {
    
    private static final Logger logger = LoggerFactory.getLogger(RocketMQConfig.class);
    
    @Value("${rocketmq.name-server:localhost:9876}")
    private String nameServer;
    
    @Value("${rocketmq.producer.group:rate-limit-producer-group}")
    private String producerGroup;
    
    @Value("${rocketmq.producer.send-message-timeout:3000}")
    private int sendMessageTimeout;
    
    @Bean
    public DefaultMQProducer defaultMQProducer() {
        DefaultMQProducer producer = new DefaultMQProducer(producerGroup);
        producer.setNamesrvAddr(nameServer);
        producer.setSendMsgTimeout(sendMessageTimeout);
        producer.setRetryTimesWhenSendFailed(3);
        producer.setRetryTimesWhenSendAsyncFailed(3);
        
        try {
            producer.start();
            logger.info("RocketMQ Producer started successfully. NameServer: {}, Group: {}", 
                       nameServer, producerGroup);
        } catch (MQClientException e) {
            logger.error("Failed to start RocketMQ Producer", e);
            throw new RuntimeException("Failed to initialize RocketMQ Producer", e);
        }
        
        return producer;
    }
}