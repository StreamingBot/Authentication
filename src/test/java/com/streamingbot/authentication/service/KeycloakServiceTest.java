package com.streamingbot.authentication.service;

import com.streamingbot.authentication.model.AuthResponse;
import com.streamingbot.authentication.model.User;
import jakarta.ws.rs.core.Response;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.keycloak.admin.client.Keycloak;
import org.keycloak.admin.client.KeycloakBuilder;
import org.keycloak.admin.client.resource.RealmResource;
import org.keycloak.admin.client.resource.UserResource;
import org.keycloak.admin.client.resource.UsersResource;
import org.keycloak.admin.client.token.TokenManager;
import org.keycloak.representations.idm.UserRepresentation;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.MockitoAnnotations;
import org.springframework.test.util.ReflectionTestUtils;
import reactor.test.StepVerifier;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.*;

class KeycloakServiceTest {

    @Mock
    private RabbitMQService rabbitMQService;

    @Mock
    private Keycloak keycloak;

    @Mock
    private RealmResource realmResource;

    @Mock
    private UsersResource usersResource;

    @Mock
    private Response createUserResponse;

    private TestableKeycloakService keycloakService;
    private AutoCloseable mocks;

    private static class TestableKeycloakService extends KeycloakService {
        private final Keycloak mockKeycloak;

        public TestableKeycloakService(RabbitMQService rabbitMQService, Keycloak mockKeycloak) {
            super(rabbitMQService);
            this.mockKeycloak = mockKeycloak;
        }

        @Override
        protected Keycloak getAdminKeycloakInstance() {
            return mockKeycloak;
        }

        @Override
        protected Keycloak getUserKeycloakInstance(String username, String password) {
            return mockKeycloak;
        }
    }

    @BeforeEach
    void setUp() {
        mocks = MockitoAnnotations.openMocks(this);
        
        // Set up configuration
        keycloakService = new TestableKeycloakService(rabbitMQService, keycloak);
        ReflectionTestUtils.setField(keycloakService, "serverUrl", "http://localhost:8080");
        ReflectionTestUtils.setField(keycloakService, "realm", "master");
        ReflectionTestUtils.setField(keycloakService, "clientId", "test-client");
        ReflectionTestUtils.setField(keycloakService, "clientSecret", "test-secret");
        ReflectionTestUtils.setField(keycloakService, "adminUsername", "admin");
        ReflectionTestUtils.setField(keycloakService, "adminPassword", "admin");

        // Set up basic Keycloak mocking
        when(keycloak.realm(anyString())).thenReturn(realmResource);
        when(realmResource.users()).thenReturn(usersResource);
    }

    @AfterEach
    void tearDown() throws Exception {
        if (mocks != null) {
            mocks.close();
        }
    }

    @Test
    void registerUser_Success() {
        User user = new User("test@test.com", "password", "John", "Doe");
        
        when(createUserResponse.getStatus()).thenReturn(201);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(createUserResponse);

        StepVerifier.create(keycloakService.registerUser(user))
            .expectNext(new AuthResponse("SUCCESS", null, "User registered successfully"))
            .verifyComplete();

        verify(usersResource).create(any(UserRepresentation.class));
    }

    @Test
    void registerUser_Failure() {
        User user = new User("test@test.com", "password", "John", "Doe");
        
        when(createUserResponse.getStatus()).thenReturn(400);
        when(usersResource.create(any(UserRepresentation.class))).thenReturn(createUserResponse);

        StepVerifier.create(keycloakService.registerUser(user))
            .expectNext(new AuthResponse("ERROR", null, "Failed to register user. Status: 400"))
            .verifyComplete();
    }

    @Test
    void deleteUserData_Success() {
        String userId = "user123";
        String authToken = "Bearer token123";
        
        UserResource userResource = mock(UserResource.class);
        UserRepresentation userRep = new UserRepresentation();
        userRep.setEmail("test@test.com");

        when(userResource.toRepresentation()).thenReturn(userRep);
        when(usersResource.get(userId)).thenReturn(userResource);

        StepVerifier.create(keycloakService.deleteUserData(userId, authToken))
            .verifyComplete();

        verify(usersResource).delete(userId);
        verify(rabbitMQService).publishUserDeletionEvent(eq(userId), eq("test@test.com"));
    }

    @Test
    void deleteUserData_UserNotFound() {
        String userId = "nonexistent";
        String authToken = "Bearer token123";

        when(usersResource.get(userId)).thenThrow(new jakarta.ws.rs.NotFoundException("User not found"));

        StepVerifier.create(keycloakService.deleteUserData(userId, authToken))
            .expectError(RuntimeException.class)
            .verify();
    }

    @Test
    void login_Success() {
        User user = new User("test@test.com", "password", null, null);
        String expectedToken = "test-token";

        TokenManager tokenManager = mock(TokenManager.class);
        when(keycloak.tokenManager()).thenReturn(tokenManager);
        when(tokenManager.getAccessTokenString()).thenReturn(expectedToken);

        StepVerifier.create(keycloakService.login(user))
            .expectNext(new AuthResponse("SUCCESS", expectedToken, "Login successful"))
            .verifyComplete();
    }

    @Test
    void login_Failure() {
        User user = new User("test@test.com", "wrong-password", null, null);
        
        when(keycloak.tokenManager()).thenThrow(new RuntimeException("Invalid credentials"));

        StepVerifier.create(keycloakService.login(user))
            .expectNext(new AuthResponse("ERROR", null, "Login failed: Invalid credentials"))
            .verifyComplete();
    }
} 