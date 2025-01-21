package com.streamingbot.authentication.config;

import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.security.config.web.server.ServerHttpSecurity;
import org.springframework.security.web.server.SecurityWebFilterChain;

@Configuration
public class SecurityConfig {

    @Bean
    public SecurityWebFilterChain springSecurityFilterChain(ServerHttpSecurity http) {
        return http
            .csrf(csrf -> csrf.disable())  // Disable CSRF
            .authorizeExchange(auth -> auth
                .pathMatchers("/auth/register", "/auth/login").permitAll()
                .pathMatchers("/actuator/**").permitAll()  // Allow actuator endpoints
                .pathMatchers("/actuator/prometheus").permitAll()  // Explicitly allow Prometheus endpoint
                .anyExchange().authenticated()
            )
            .build();
    }
} 