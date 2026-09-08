package com.opsflow.common.exception;

import com.opsflow.common.response.ApiResponse;
import com.opsflow.common.response.ErrorResponse;
import com.opsflow.common.response.FieldErrorDetail;
import jakarta.servlet.http.HttpServletRequest;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;
import org.springframework.web.servlet.NoHandlerFoundException;

import java.time.Instant;
import java.util.List;

@RestControllerAdvice
public class GlobalExceptionHandler {

    private static final Logger log = LoggerFactory.getLogger(GlobalExceptionHandler.class);

    @ExceptionHandler(BusinessException.class)
    public ResponseEntity<ApiResponse<Void>> handleBusinessException(BusinessException ex, HttpServletRequest request) {
        log.warn("Business exception occurred at path '{}': {}", request.getRequestURI(), ex.getMessage());
        ErrorCode errorCode = ex.getErrorCode();
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(errorCode.getHttpStatus().value())
            .error(errorCode.getCode())
            .message(ex.getMessage())
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(errorCode.getHttpStatus()).body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(MethodArgumentNotValidException.class)
    public ResponseEntity<ApiResponse<Void>> handleValidationException(MethodArgumentNotValidException ex, HttpServletRequest request) {
        log.warn("Validation error at path '{}': {}", request.getRequestURI(), ex.getMessage());
        BindingResult bindingResult = ex.getBindingResult();
        List<FieldErrorDetail> fieldErrors = bindingResult.getFieldErrors().stream()
            .map(fieldError -> new FieldErrorDetail(
                fieldError.getField(),
                fieldError.getDefaultMessage(),
                fieldError.getRejectedValue()
            ))
            .toList();

        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.BAD_REQUEST.value())
            .error(ErrorCode.VALIDATION_ERROR.getCode())
            .message("Request validation failed")
            .path(request.getRequestURI())
            .details(fieldErrors)
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.BAD_REQUEST).body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(NoHandlerFoundException.class)
    public ResponseEntity<ApiResponse<Void>> handleNoHandlerFoundException(NoHandlerFoundException ex, HttpServletRequest request) {
        log.warn("No handler found for {} {}", ex.getHttpMethod(), ex.getRequestURL());
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.NOT_FOUND.value())
            .error(ErrorCode.RESOURCE_NOT_FOUND.getCode())
            .message(String.format("Path '%s' not found", ex.getRequestURL()))
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.NOT_FOUND).body(ApiResponse.error(errorResponse));
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ApiResponse<Void>> handleUnexpectedException(Exception ex, HttpServletRequest request) {
        log.error("Unhandled exception at path '{}': ", request.getRequestURI(), ex);
        ErrorResponse errorResponse = ErrorResponse.builder()
            .status(HttpStatus.INTERNAL_SERVER_ERROR.value())
            .error(ErrorCode.INTERNAL_SERVER_ERROR.getCode())
            .message("An unexpected internal error occurred")
            .path(request.getRequestURI())
            .timestamp(Instant.now())
            .build();

        return ResponseEntity.status(HttpStatus.INTERNAL_SERVER_ERROR).body(ApiResponse.error(errorResponse));
    }
}
