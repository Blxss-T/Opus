package com.opsflow.auth.dto;

/** Generic single-message payload for auth endpoints that return no domain data. */
public record MessageResponse(String message) {}
