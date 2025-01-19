package com.streamingbot.authentication.service;

import com.streamingbot.authentication.model.UserDeletionEvent;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.test.util.ReflectionTestUtils;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class RabbitMQServiceTest {

    @Mock
    private RabbitTemplate rabbitTemplate;

    @InjectMocks
    private RabbitMQService rabbitMQService;

    private final String exchange = "user-events-exchange";
    private final String routingKey = "user-deletion-key";

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
        ReflectionTestUtils.setField(rabbitMQService, "userEventsExchange", exchange);
        ReflectionTestUtils.setField(rabbitMQService, "userDeletionRoutingKey", routingKey);
    }

    @Test
    void publishUserDeletionEvent_Success() {
        String userId = "user123";
        String userEmail = "test@test.com";

        rabbitMQService.publishUserDeletionEvent(userId, userEmail);

        verify(rabbitTemplate).convertAndSend(
            eq(exchange),
            eq(routingKey),
            any(UserDeletionEvent.class)
        );
    }

    @Test
    void publishUserDeletionEvent_Failure() {
        String userId = "user123";
        String userEmail = "test@test.com";

        doThrow(new RuntimeException("Connection failed"))
            .when(rabbitTemplate)
            .convertAndSend(anyString(), anyString(), any(UserDeletionEvent.class));

        assertThrows(RuntimeException.class, () -> 
            rabbitMQService.publishUserDeletionEvent(userId, userEmail)
        );
    }
} 