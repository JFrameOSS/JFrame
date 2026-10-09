package io.github.jframe.exception.resource;

import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.Locale;
import jakarta.ws.rs.core.Response;

/**
 * Runtime-neutral helpers for RFC 9457 Problem Details members.
 */
public final class ProblemDetails {

    /** Problem Details media type. */
    public static final String MEDIA_TYPE = "application/problem+json";

    /** Plain JSON media type, used only when the client accepts JSON but not Problem Details. */
    public static final String JSON_MEDIA_TYPE = "application/json";

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

    /**
     * Picks the error media type for an {@code Accept} header; never fails negotiation.
     *
     * @param accept the raw {@code Accept} header, may be {@code null}
     * @return {@link #JSON_MEDIA_TYPE} only when JSON is acceptable and Problem Details is not, else {@link #MEDIA_TYPE}
     */
    public static String negotiateMediaType(final String accept) {
        if (accept == null || accept.isBlank() || accepts(accept, "problem+json") || !accepts(accept, "json")) {
            return MEDIA_TYPE;
        }
        return JSON_MEDIA_TYPE;
    }

    /** Whether the most specific range matching {@code application/subtype} has a non-zero quality. */
    private static boolean accepts(final String accept, final String subtype) {
        int bestSpecificity = -1;
        double quality = 0;
        for (final String range : accept.split(",")) {
            final String[] parts = range.split(";");
            final String mediaRange = parts[0].trim().toLowerCase(Locale.ROOT);
            final int specificity = specificity(mediaRange, subtype);
            if (specificity > bestSpecificity) {
                bestSpecificity = specificity;
                quality = quality(parts);
            }
        }
        return quality > 0;
    }

    private static int specificity(final String mediaRange, final String subtype) {
        return switch (mediaRange) {
            case "application/*" -> 1;
            case "*/*", "*" -> 0;
            default -> ("application/" + subtype).equals(mediaRange) ? 2 : -1;
        };
    }

    private static double quality(final String... parameters) {
        double quality = 1;
        for (int i = 1; i < parameters.length; i++) {
            final String[] parameter = parameters[i].split("=", 2);
            if (parameter.length == 2 && "q".equalsIgnoreCase(parameter[0].trim())) {
                try {
                    quality = Double.parseDouble(parameter[1].trim());
                } catch (final NumberFormatException ignored) {
                    quality = 0;
                }
            }
        }
        return quality;
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
