package com.streamingbot.authentication.exception;

import com.streamingbot.authentication.model.AuthResponse;
import org.junit.jupiter.api.Test;
import org.springframework.http.ResponseEntity;
import reactor.test.StepVerifier;

class GlobalExceptionHandlerTest {

    private final GlobalExceptionHandler exceptionHandler = new GlobalExceptionHandler();

    @Test
    void handleAllExceptions_ReturnsErrorResponse() {
        Exception testException = new RuntimeException("Test error message");
        
        StepVerifier.create(exceptionHandler.handleAllExceptions(testException))
            .expectNext(
                ResponseEntity.badRequest()
                    .body(new AuthResponse("ERROR", null, "Test error message"))
            )
            .verifyComplete();
    }
} 