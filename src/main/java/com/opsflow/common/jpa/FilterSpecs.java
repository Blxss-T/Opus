package com.opsflow.common.jpa;

import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;

import java.util.Arrays;
import java.util.Locale;
import java.util.UUID;

/**
 * Small helper for building dynamic list-filter Specifications in a
 * PostgreSQL-safe way: null/blank filters are simply omitted from the
 * predicate tree instead of relying on `:param IS NULL` SQL constructs.
 */
public final class FilterSpecs {

    private FilterSpecs() {
    }

    @SafeVarargs
    public static <T> Specification<T> and(Specification<T>... specs) {
        Specification<T> result = null;
        for (Specification<T> spec : specs) {
            if (spec == null) {
                continue;
            }
            result = result == null ? spec : result.and(spec);
        }
        return result != null ? result : (root, query, cb) -> cb.conjunction();
    }

    /** Tenant scope: root.organization.id = :organizationId */
    public static <T> Specification<T> organizationIs(UUID organizationId) {
        return (root, query, cb) -> cb.equal(root.get("organization").get("id"), organizationId);
    }

    /**
     * Case-insensitive "contains" search across the given string fields.
     * Returns null when the term is null/blank so it drops out of the AND chain.
     * LIKE wildcards in user input are escaped.
     */
    public static <T> Specification<T> ilikeAny(String term, String... fields) {
        if (term == null || term.isBlank()) {
            return null;
        }
        String pattern = "%" + escapeLike(term.trim().toLowerCase(Locale.ROOT)) + "%";
        return (root, query, cb) -> cb.or(
            Arrays.stream(fields)
                .map(field -> cb.like(cb.lower(root.<String>get(field)), pattern, '\\'))
                .toArray(jakarta.persistence.criteria.Predicate[]::new)
        );
    }

    /** Exact equality filter, skipped when value is null. */
    public static <T> Specification<T> equalsValue(String field, Object value) {
        if (value == null) {
            return null;
        }
        return (root, query, cb) -> cb.equal(root.get(field), value);
    }

    /** Case-insensitive exact equality for string values, skipped when blank. */
    public static <T> Specification<T> equalsIgnoreCase(String field, String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        String normalized = value.trim().toLowerCase(Locale.ROOT);
        return (root, query, cb) -> cb.equal(cb.lower(root.<String>get(field)), normalized);
    }

    /** Applies the given default sort when the request does not carry an explicit sort. */
    public static Pageable withDefaultSort(Pageable pageable, Sort.Order... defaultOrders) {
        if (pageable.getSort().isSorted()) {
            return pageable;
        }
        return org.springframework.data.domain.PageRequest.of(
            pageable.getPageNumber(),
            pageable.getPageSize(),
            Sort.by(defaultOrders)
        );
    }

    private static String escapeLike(String input) {
        return input
            .replace("\\", "\\\\")
            .replace("%", "\\%")
            .replace("_", "\\_");
    }
}
