package com.opsflow.common.idempotency;

/**
 * Outcome of a create that accepts an idempotency key: either the newly
 * created resource, or the one an earlier request with the same key created.
 */
public record IdempotentResult<T>(T value, boolean replayed) {

    public static <T> IdempotentResult<T> created(T value) {
        return new IdempotentResult<>(value, false);
    }

    public static <T> IdempotentResult<T> replayed(T value) {
        return new IdempotentResult<>(value, true);
    }
}
