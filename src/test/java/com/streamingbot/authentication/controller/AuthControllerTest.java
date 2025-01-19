package com.streamingbot.authentication.controller;

import com.streamingbot.authentication.model.AuthResponse;
import com.streamingbot.authentication.model.User;
import com.streamingbot.authentication.service.KeycloakService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockitoAnnotations;
import reactor.core.publisher.Mono;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

class AuthControllerTest {

    @Mock
    private KeycloakService keycloakService;

    @InjectMocks
    private AuthController authController;

    @BeforeEach
    void setUp() {
        MockitoAnnotations.openMocks(this);
    }

    @Test
    void registerUser_Success() {
        User user = new User("test@test.com", "password", "John", "Doe");
        AuthResponse expectedResponse = new AuthResponse("SUCCESS", null, "User registered successfully");
        
        when(keycloakService.registerUser(any(User.class)))
            .thenReturn(Mono.just(expectedResponse));

        StepVerifier.create(authController.registerUser(user))
            .expectNext(expectedResponse)
            .verifyComplete();

        verify(keycloakService).registerUser(user);
    }

    @Test
    void login_Success() {
        User user = new User("test@test.com", "password", null, null);
        AuthResponse expectedResponse = new AuthResponse("SUCCESS", "token123", "Login successful");
        
        when(keycloakService.login(any(User.class)))
            .thenReturn(Mono.just(expectedResponse));

        StepVerifier.create(authController.login(user))
            .expectNext(expectedResponse)
            .verifyComplete();

        verify(keycloakService).login(user);
    }

    @Test
    void deleteUserData_Success() {
        String userId = "user123";
        String authToken = "Bearer token123";
        
        when(keycloakService.deleteUserData(userId, authToken))
            .thenReturn(Mono.empty());

        StepVerifier.create(authController.deleteUserData(userId, authToken))
            .verifyComplete();

        verify(keycloakService).deleteUserData(userId, authToken);
    }
} 