package io.github.jframe.exception.resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import jakarta.ws.rs.core.Response;

/**
 * Runtime-neutral helpers for RFC 9457 Problem Details members.
 */
public final class ProblemDetails {

    /** Problem Details media type. */
    public static final String MEDIA_TYPE = "application/problem+json";

    private ProblemDetails() {
    }

    /** Builds the {@code type} URI: base URI plus percent-encoded error code; {@code null} without a base or code. */
    public static String type(final String baseUri, final String errorCode) {
        if (errorCode == null || baseUri == null || baseUri.isBlank()) {
            return null;
        }
        return baseUri + URLEncoder.encode(errorCode, StandardCharsets.UTF_8).replace("+", "%20");
    }

    /** Returns the HTTP reason phrase for the status, used as {@code title}. */
    public static String title(final int status) {
        final Response.Status known = Response.Status.fromStatusCode(status);
        return known == null ? familyLabel(Response.Status.Family.familyOf(status)) : known.getReasonPhrase();
    }

    private static String familyLabel(final Response.Status.Family family) {
        return switch (family) {
            case INFORMATIONAL -> "Informational";
            case SUCCESSFUL -> "Success";
            case REDIRECTION -> "Redirection";
            case CLIENT_ERROR -> "Client Error";
            case SERVER_ERROR -> "Server Error";
            case OTHER -> "Unknown Status";
        };
    }

    /**
     * Applies {@code status}, {@code title}, {@code instance} and {@code type} to the resource.
     *
     * @param resource the resource
     * @param status   the HTTP status
     * @param instance the request path, without query
     * @param baseUri  the {@code type} base URI
     */
    public static void apply(final ErrorResponseResource resource, final int status, final String instance, final String baseUri) {
        resource.setStatus(status);
        resource.setTitle(title(status));
        resource.setInstance(instance);
        resource.setType(type(baseUri, resource.getErrorCode()));
    }
}
