package io.github.jframe.logging.logger;

import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import lombok.RequiredArgsConstructor;

import org.apache.commons.lang3.StringUtils;

/**
 * Extracts and masks request and response bodies for HTTP logging.
 *
 * <p>Delegates body retrieval to the caching context wrappers and applies
 * password masking before returning the result.
 */
@RequiredArgsConstructor
public class HttpRequestResponseBodyLogger {

    /** Masks sensitive field values in body strings. */
    private final PasswordMasker passwordMasker;

    /**
     * Returns the masked request body.
     *
     * @param cachingRequest the caching request context
     * @return masked body string, or empty string if body is null or empty
     */
    public String getRequestBody(final CachingRequestContext cachingRequest) {
        final String body = cachingRequest.getCachedBodyAsString();
        if (StringUtils.isEmpty(body)) {
            return "";
        }
        return passwordMasker.maskPasswordsIn(body);
    }

    /**
     * Returns the masked response body.
     *
     * @param cachingResponse the caching response context
     * @return masked body string, or empty string if body is null or empty
     */
    public String getResponseBody(final CachingResponseContext cachingResponse) {
        final String body = cachingResponse.getCachedBodyAsString();
        if (StringUtils.isEmpty(body)) {
            return "";
        }
        return passwordMasker.maskPasswordsIn(body);
    }

    /**
     * Returns the masked response body, truncated to {@code maxLength} characters.
     *
     * <p>If {@code maxLength} is {@code -1}, the full body is returned without truncation.
     * Truncation is applied after masking.
     *
     * @param cachingResponse the caching response context
     * @param maxLength       maximum number of characters to return; {@code -1} means unlimited
     * @return masked (and possibly truncated) body string, or empty string if body is absent
     */
    public String getResponseBody(final CachingResponseContext cachingResponse, final int maxLength) {
        final String masked = getResponseBody(cachingResponse);
        final boolean truncate = maxLength != -1 && masked.length() > maxLength;
        return truncate ? masked.substring(0, maxLength) : masked;
    }
}
