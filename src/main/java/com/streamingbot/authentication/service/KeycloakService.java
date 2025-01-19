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
import jakarta.ws.rs.NotFoundException;

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

    private final RabbitMQService rabbitMQService;
    
    public KeycloakService(RabbitMQService rabbitMQService) {
        this.rabbitMQService = rabbitMQService;
    }
    
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

    public Mono<AuthResponse> login(User user) {
        return Mono.fromCallable(() -> {
            try {
                logger.info("Attempting login for user: {}", user.email());
                logger.debug("Connecting to Keycloak server at: {}", serverUrl);
                logger.debug("Realm: {}", realm);
                logger.debug("Client ID: {}", clientId);
                logger.debug("Client Secret: {}", clientSecret);
                logger.debug("Username: {}", user.email());
                logger.debug("Password: {}", user.password());
                
                Keycloak keycloak = KeycloakBuilder.builder()
                        .serverUrl(serverUrl)
                        .realm(realm)
                        .clientId(clientId)
                        .clientSecret(clientSecret)
                        .grantType("password")
                        .username(user.email())
                        .password(user.password())
                        .build();

                logger.debug("Attempting to get access token");
                String token = keycloak.tokenManager().getAccessTokenString();
                logger.info("Successfully logged in user: {}", user.email());
                return new AuthResponse("SUCCESS", token, "Login successful");
            } catch (Exception e) {
                logger.error("Login failed for user: {}. Error type: {}. Message: {}", 
                    user.email(), e.getClass().getSimpleName(), e.getMessage());
                if (e.getCause() != null) {
                    logger.error("Caused by: {}", e.getCause().getMessage());
                }
                return new AuthResponse("ERROR", null, "Login failed: " + e.getMessage());
            }
        }).subscribeOn(Schedulers.boundedElastic());
    }

    public Mono<Void> deleteUserData(String userId, String authToken) {
        return Mono.<Void>create(sink -> {
            try {
                logger.info("Attempting to delete user data for userId: {}", userId);
                logger.debug("Connecting to Keycloak server for user deletion");
                
                Keycloak keycloak = getKeycloakInstance();
                
                try {
                    UserRepresentation user = keycloak.realm(realm).users().get(userId).toRepresentation();
                    logger.debug("Found user to delete: {}", user.getEmail());
                    
                    // Delete the user from Keycloak
                    keycloak.realm(realm).users().delete(userId);
                    logger.info("Successfully deleted user data from Keycloak for userId: {}", userId);
                    
                    // Publish deletion event using RabbitMQService
                    rabbitMQService.publishUserDeletionEvent(userId, user.getEmail());
                    
                    sink.success();
                    
                } catch (NotFoundException e) {
                    logger.error("User not found with userId: {}", userId);
                    sink.error(new RuntimeException("User not found"));
                }
                
            } catch (Exception e) {
                logger.error("Error deleting user data", e);
                sink.error(new RuntimeException("Failed to delete user data: " + e.getMessage()));
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