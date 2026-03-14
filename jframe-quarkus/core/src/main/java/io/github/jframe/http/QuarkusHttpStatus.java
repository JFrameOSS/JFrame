package io.github.jframe.http;

import jakarta.ws.rs.core.Response;

/**
 * Utility class for converting JFrame {@link HttpStatusCode} values to JAX-RS {@link Response.Status}.
 */
public final class QuarkusHttpStatus {

    private QuarkusHttpStatus() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Converts a JFrame {@link HttpStatusCode} to the corresponding JAX-RS {@link Response.Status}.
     *
     * <p>Returns {@link Response.Status#INTERNAL_SERVER_ERROR} when {@code httpStatusCode} is {@code null}
     * or cannot be mapped.
     *
     * @param httpStatusCode the JFrame status code to convert, may be {@code null}
     * @return the matching JAX-RS {@link Response.Status}
     */
    public static Response.Status toJaxRsStatus(final HttpStatusCode httpStatusCode) {
        if (httpStatusCode == null) {
            return Response.Status.INTERNAL_SERVER_ERROR;
        }
        final Response.Status status = Response.Status.fromStatusCode(httpStatusCode.getCode());
        return status != null ? status : Response.Status.INTERNAL_SERVER_ERROR;
    }
}
