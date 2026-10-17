package com.opsflow.common.idempotency;

import com.opsflow.common.exception.BusinessException;
import com.opsflow.common.exception.ErrorCode;
import com.opsflow.common.response.ApiResponse;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;

/** Shared handling of the {@code Idempotency-Key} request header. */
public final class IdempotencyKeys {

    public static final String HEADER = "Idempotency-Key";
    public static final String REPLAYED_HEADER = "Idempotency-Replayed";
    public static final String DESCRIPTION = "Optional unique value (up to 100 characters, e.g. a UUID) that makes "
        + "this request safe to retry. Sending the same key again returns the order created the first time, with "
        + "status 200 and `Idempotency-Replayed: true`, instead of creating a duplicate. Keys are scoped to the organization.";

    private static final int MAX_LENGTH = 100;

    private IdempotencyKeys() {
    }

    /** Trims the header value; null when absent. Rejects blank or over-long keys. */
    public static String normalize(String headerValue) {
        if (headerValue == null) {
            return null;
        }
        String key = headerValue.trim();
        if (key.isEmpty() || key.length() > MAX_LENGTH) {
            throw new BusinessException(
                ErrorCode.VALIDATION_ERROR,
                HEADER + " must be between 1 and " + MAX_LENGTH + " characters"
            );
        }
        return key;
    }

    /** 201 for a new resource; 200 plus the replay header for a repeated key. */
    public static <T> ResponseEntity<ApiResponse<T>> toResponse(IdempotentResult<T> result) {
        if (result.replayed()) {
            return ResponseEntity.ok()
                .header(REPLAYED_HEADER, "true")
                .body(ApiResponse.success(result.value()));
        }
        return ResponseEntity.status(HttpStatus.CREATED).body(ApiResponse.success(result.value()));
    }
}
