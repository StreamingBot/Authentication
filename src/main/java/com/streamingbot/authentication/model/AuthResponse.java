package com.streamingbot.authentication.model;

public record AuthResponse(
    String status,
    String token,
    String message
) {} 