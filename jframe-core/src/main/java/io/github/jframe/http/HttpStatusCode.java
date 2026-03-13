package io.github.jframe.http;

/**
 * Enum representing common HTTP status codes with their numeric code and human-readable reason.
 */
public enum HttpStatusCode {

    OK(200, "OK"),
    BAD_REQUEST(400, "Bad Request"),
    UNAUTHORIZED(401, "Unauthorized"),
    FORBIDDEN(403, "Forbidden"),
    NOT_FOUND(404, "Not Found"),
    TOO_MANY_REQUESTS(429, "Too Many Requests"),
    INTERNAL_SERVER_ERROR(500, "Internal Server Error");

    private final int code;

    private final String reason;

    HttpStatusCode(final int code, final String reason) {
        this.code = code;
        this.reason = reason;
    }

    /**
     * Returns the numeric HTTP status code.
     *
     * @return the status code
     */
    public int getCode() {
        return code;
    }

    /**
     * Returns the human-readable reason phrase.
     *
     * @return the reason phrase
     */
    public String getReason() {
        return reason;
    }

    /**
     * Returns the {@code HttpStatusCode} constant for the given numeric code.
     *
     * @param code the numeric HTTP status code
     * @return the matching {@code HttpStatusCode}
     * @throws IllegalArgumentException if no constant matches the given code
     */
    public static HttpStatusCode valueOf(final int code) {
        for (final HttpStatusCode status : values()) {
            if (status.code == code) {
                return status;
            }
        }
        throw new IllegalArgumentException("Unknown HTTP status code: " + code);
    }

    /**
     * Returns {@code true} if this status code is in the 2xx (Successful) range.
     *
     * @return {@code true} if 2xx
     */
    public boolean is2xxSuccessful() {
        return code >= 200 && code < 300;
    }

    /**
     * Returns {@code true} if this status code is in the 4xx (Client Error) range.
     *
     * @return {@code true} if 4xx
     */
    public boolean is4xxClientError() {
        return code >= 400 && code < 500;
    }

    /**
     * Returns {@code true} if this status code is in the 5xx (Server Error) range.
     *
     * @return {@code true} if 5xx
     */
    public boolean is5xxServerError() {
        return code >= 500 && code < 600;
    }
}
