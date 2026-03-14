package io.github.jframe.logging.filter;

/**
 * Priority constants for Quarkus JAX-RS container filters.
 *
 * <p>Lower numbers run first. Use these constants with {@code @Priority} annotations on
 * filter classes to control the order of execution.
 */
public final class FilterPriorities {

    /** Priority for the tracing response filter (runs first). */
    public static final int TRACING_RESPONSE = 50;

    /** Priority for the transaction ID filter. */
    public static final int TRANSACTION_ID = 100;

    /** Priority for the request ID filter. */
    public static final int REQUEST_ID = 200;

    /** Priority for the request duration filter. */
    public static final int REQUEST_DURATION = 300;

    /** Priority for the request/response log filter (runs last). */
    public static final int REQUEST_RESPONSE_LOG = 400;

    private FilterPriorities() {
        throw new UnsupportedOperationException("Utility class");
    }
}
