package com.opsflow.common.exception;

import org.springframework.http.HttpStatus;

public enum ErrorCode {

    INTERNAL_SERVER_ERROR("ERR_500", "An unexpected error occurred", HttpStatus.INTERNAL_SERVER_ERROR),
    VALIDATION_ERROR("ERR_400", "Request validation failed", HttpStatus.BAD_REQUEST),
    RESOURCE_NOT_FOUND("ERR_404", "Requested resource was not found", HttpStatus.NOT_FOUND),
    UNAUTHORIZED("ERR_401", "Authentication failure or missing credentials", HttpStatus.UNAUTHORIZED),
    FORBIDDEN("ERR_403", "Access denied for requested operation", HttpStatus.FORBIDDEN),
    BUSINESS_RULE_VIOLATION("ERR_422", "Business rule violation occurred", HttpStatus.UNPROCESSABLE_ENTITY);

    private final String code;
    private final String defaultMessage;
    private final HttpStatus httpStatus;

    ErrorCode(String code, String defaultMessage, HttpStatus httpStatus) {
        this.code = code;
        this.defaultMessage = defaultMessage;
        this.httpStatus = httpStatus;
    }

    public String getCode() {
        return code;
    }

    public String getDefaultMessage() {
        return defaultMessage;
    }

    public HttpStatus getHttpStatus() {
        return httpStatus;
    }
}
