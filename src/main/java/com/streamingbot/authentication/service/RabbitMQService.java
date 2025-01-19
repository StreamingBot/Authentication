package com.streamingbot.authentication.service;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import com.streamingbot.authentication.model.UserDeletionEvent;

@Service
public class RabbitMQService {
    private static final Logger logger = LoggerFactory.getLogger(RabbitMQService.class);
    
    private final RabbitTemplate rabbitTemplate;
    
    @Value("${rabbitmq.exchange.user-events}")
    private String userEventsExchange;
    
    @Value("${rabbitmq.routing-key.user-deletion}")
    private String userDeletionRoutingKey;
    
    public RabbitMQService(RabbitTemplate rabbitTemplate) {
        this.rabbitTemplate = rabbitTemplate;
    }
    
    public void publishUserDeletionEvent(String userId, String userEmail) {
        try {
            UserDeletionEvent event = new UserDeletionEvent(
                userId,
                userEmail,
                System.currentTimeMillis()
            );
            
            logger.info("Publishing user deletion event for userId: {}", userId);
            rabbitTemplate.convertAndSend(userEventsExchange, userDeletionRoutingKey, event);
            logger.debug("Successfully published user deletion event");
        } catch (Exception e) {
            logger.error("Failed to publish user deletion event", e);
            throw new RuntimeException("Failed to publish deletion event: " + e.getMessage());
        }
    }
} 