package io.github.jframe.http;

import java.util.Objects;

import org.springframework.http.HttpStatus;

/**
 * Utility class for converting between jframe-core's {@link HttpStatusCode} enum
 * and Spring's {@link HttpStatus}.
 *
 * <p>Provides bidirectional mapping for all 7 status codes supported by jframe-core,
 * and a convenience method returning Spring's {@link org.springframework.http.HttpStatusCode} interface.
 */
public final class SpringHttpStatus {

    private SpringHttpStatus() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a jframe-core {@link HttpStatusCode} to a Spring {@link HttpStatus}.
     *
     * @param httpStatusCode the jframe status code; must not be {@code null}
     * @return the corresponding Spring {@link HttpStatus}
     * @throws NullPointerException if {@code httpStatusCode} is {@code null}
     */
    public static HttpStatus toSpringHttpStatus(final HttpStatusCode httpStatusCode) {
        Objects.requireNonNull(httpStatusCode, "httpStatusCode must not be null");
        return HttpStatus.valueOf(httpStatusCode.getCode());
    }

    /**
     * Converts a Spring {@link HttpStatus} to a jframe-core {@link HttpStatusCode}.
     *
     * @param httpStatus the Spring HTTP status; must not be {@code null}
     * @return the corresponding jframe {@link HttpStatusCode}
     * @throws NullPointerException     if {@code httpStatus} is {@code null}
     * @throws IllegalArgumentException if the Spring status has no jframe mapping
     */
    public static HttpStatusCode fromSpringHttpStatus(final HttpStatus httpStatus) {
        Objects.requireNonNull(httpStatus, "httpStatus must not be null");
        try {
            return HttpStatusCode.valueOf(httpStatus.value());
        } catch (IllegalArgumentException e) {
            throw new IllegalArgumentException(
                "No jframe HttpStatusCode mapping for Spring HttpStatus: " + httpStatus,
                e
            );
        }
    }

    /**
     * Converts a jframe-core {@link HttpStatusCode} to Spring's {@link org.springframework.http.HttpStatusCode}
     * interface.
     *
     * @param httpStatusCode the jframe status code; must not be {@code null}
     * @return the corresponding Spring {@link org.springframework.http.HttpStatusCode}
     * @throws NullPointerException if {@code httpStatusCode} is {@code null}
     */
    public static org.springframework.http.HttpStatusCode toSpringHttpStatusCode(final HttpStatusCode httpStatusCode) {
        return toSpringHttpStatus(httpStatusCode);
    }
}
