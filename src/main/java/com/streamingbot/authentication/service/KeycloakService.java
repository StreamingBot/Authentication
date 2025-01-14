package com.streamingbot.authentication.service;

import com.streamingbot.authentication.model.AuthResponse;
import com.streamingbot.authentication.model.User;
import jakarta.annotation.PostConstruct;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.representations.idm.CredentialRepresentation;
import org.keycloak.representations.idm.UserRepresentation;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;
import reactor.core.publisher.Mono;
import reactor.core.scheduler.Schedulers;

import java.util.Collections;

@Service
public class KeycloakService {
    
    private static final Logger logger = LoggerFactory.getLogger(KeycloakService.class);
    
    @Value("${keycloak.auth-server-url}")
    private String serverUrl;
    
    @Value("${keycloak.realm}")
    private String realm;
    
    @Value("${keycloak.resource}")
    private String clientId;
    
    @Value("${keycloak.credentials.secret}")
    private String clientSecret;

    @Value("${keycloak.admin.username}")
    private String adminUsername;

    @Value("${keycloak.admin.password}")
    private String adminPassword;

    public Mono<AuthResponse> registerUser(User user) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Attempting to register user with email: {}", user.email());
                logger.debug("Connecting to Keycloak server at: {}", serverUrl);
                
                Keycloak keycloak = getKeycloakInstance();
                
                UserRepresentation userRep = new UserRepresentation();
                userRep.setEnabled(true);
                userRep.setUsername(user.email());
                userRep.setEmail(user.email());
                userRep.setFirstName(user.firstName());
                userRep.setLastName(user.lastName());
                
                CredentialRepresentation credential = new CredentialRepresentation();
                credential.setType(CredentialRepresentation.PASSWORD);
                credential.setValue(user.password());
                credential.setTemporary(false);
                userRep.setCredentials(Collections.singletonList(credential));

                logger.debug("Attempting to create user in realm: {}", realm);
                var response = keycloak.realm(realm).users().create(userRep);
                
                if (response.getStatus() == 201) {
                    logger.info("Successfully registered user: {}", user.email());
                    return new AuthResponse("SUCCESS", null, "User registered successfully");
                }
                logger.error("Failed to register user. Status: {}", response.getStatus());
                return new AuthResponse("ERROR", null, "Failed to register user. Status: " + response.getStatus());
            } catch (Exception e) {
                logger.error("Error registering user", e);
                return new AuthResponse("ERROR", null, "Error: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<AuthResponse> login(String email, String password) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Attempting login for user: {}", email);
                logger.debug("Connecting to Keycloak server at: {}", serverUrl);
                
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("password")
                        .username(email)
                        .password(password)
                        .build();

                String token = keycloak.tokenManager().getAccessTokenString();
                logger.info("Successfully logged in user: {}", email);
                return new AuthResponse("SUCCESS", token, "Login successful");
            } catch (Exception e) {
                logger.error("Login failed: {}", e.getMessage(), e);
                return new AuthResponse("ERROR", null, "Invalid credentials: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    private Keycloak getKeycloakInstance() {
        logger.debug("Creating admin Keycloak instance");
        return KeycloakBuilder.builder()
                .serverUrl(serverUrl)
                .realm("master")
                .clientId("admin-cli")
                .username(adminUsername)
                .password(adminPassword)
                .build();
    }

    @PostConstruct
    private void validateConfiguration() {
        logger.info("Validating Keycloak configuration:");
        logger.info("Server URL: {}", serverUrl);
        logger.info("Realm: {}", realm);
        logger.info("Client ID: {}", clientId);
        logger.info("Admin Username: {}", adminUsername);
    }
} 