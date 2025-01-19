package com.streamingbot.authentication.controller;

import com.streamingbot.authentication.model.AuthResponse;
import com.streamingbot.authentication.model.User;
import com.streamingbot.authentication.service.KeycloakService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.MediaType;
import org.springframework.web.bind.annotation.*;
import reactor.core.publisher.Mono;

@RestController
@RequestMapping("/auth")
public class AuthController {

    @Autowired
    private KeycloakService keycloakService;

    @PostMapping(value = "/register", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AuthResponse> registerUser(@RequestBody User user) {
        return keycloakService.registerUser(user);
    }

    @PostMapping(value = "/login", produces = MediaType.APPLICATION_JSON_VALUE)
    public Mono<AuthResponse> login(@RequestBody User user) {
        return keycloakService.login(user);
    }

    @DeleteMapping(value = "/user/{userId}")
    public Mono<Void> deleteUserData(@PathVariable String userId, @RequestHeader("Authorization") String authToken) {
        return keycloakService.deleteUserData(userId, authToken);
    }
} 