package com.streamingbot.authentication.model;

public record UserDeletionEvent(
    String userId,
    String userEmail,
    long timestamp
) {} 