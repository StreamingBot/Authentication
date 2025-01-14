package com.streamingbot.authentication.exception;

import com.streamingbot.authentication.model.AuthResponse;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import reactor.core.publisher.Mono;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(Exception.class)
    public Mono<ResponseEntity<AuthResponse>> handleAllExceptions(Exception ex) {
        return Mono.just(
            ResponseEntity.badRequest()
                .body(new AuthResponse("ERROR", null, ex.getMessage()))
        );
    }
} 