package com.streamingbot.authentication.model;

public record User(
    String email,      // email is username
    String password,
    String firstName,
    String lastName
) {} 