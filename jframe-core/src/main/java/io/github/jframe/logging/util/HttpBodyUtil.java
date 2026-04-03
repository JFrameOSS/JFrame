package io.github.jframe.logging.util;

import io.github.jframe.logging.masker.type.PasswordMasker;

/**
 * Utility class for HTTP body operations.
 *
 * <p>Provides common functionality for processing HTTP request and response bodies,
 * including compression, truncation, and password masking for logging purposes.
 */
public final class HttpBodyUtil {

    private HttpBodyUtil() {
        // Utility class, no instantiation
    }

    /**
     * Compresses (truncates) a body content string for logging purposes.
     *
     * <p>If the body content exceeds the specified maximum length, it will be truncated
     * and a summary will be appended indicating the original length and a hash of the content.
     *
     * @param bodyContent the body content to compress
     * @param maxLength   the maximum length allowed. Use -1 for unlimited length.
     * @return the compressed body content, or the original content if within limits
     */
    public static String compressBody(final String bodyContent, final int maxLength) {
        if (bodyContent == null || maxLength == -1 || bodyContent.length() <= maxLength) {
            return bodyContent;
        }

        final String truncated = bodyContent.substring(0, maxLength);
        final String hash = Integer.toHexString(bodyContent.hashCode());
        return truncated + "... [TRUNCATED: " + bodyContent.length() + " chars, hash=" + hash + "]";
    }

    /**
     * Compresses and masks passwords in a body content string for secure logging.
     *
     * <p>This method first compresses (truncates) the body content if it exceeds the maximum length,
     * then masks any sensitive fields (like passwords) to prevent them from appearing in logs.
     *
     * @param bodyContent    the body content to compress and mask
     * @param maxLength      the maximum length allowed. Use -1 for unlimited length.
     * @param passwordMasker the password masker to use for masking sensitive fields
     * @return the compressed and masked body content
     */
    public static String compressAndMaskBody(final String bodyContent, final int maxLength, final PasswordMasker passwordMasker) {
        if (bodyContent == null) {
            return null;
        }

        // First compress, then mask (masking after compression is more efficient)
        final String compressed = compressBody(bodyContent, maxLength);
        return passwordMasker != null ? passwordMasker.maskPasswordsIn(compressed) : compressed;
    }
}
