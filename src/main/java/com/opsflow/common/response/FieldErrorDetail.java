package com.opsflow.common.response;

public record FieldErrorDetail(
    String field,
    String message,
    Object rejectedValue
) {}
