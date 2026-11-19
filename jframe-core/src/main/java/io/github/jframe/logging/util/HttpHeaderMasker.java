package io.github.jframe.logging.util;

import io.github.jframe.logging.masker.type.PasswordMasker;

import java.util.Locale;
import java.util.Set;

/**
 * Masks HTTP header values for logging: credential headers and headers named after a configured fields-to-mask entry.
 */
public final class HttpHeaderMasker {

    private static final String MASKED_VALUE = "***";

    private static final Set<String> CREDENTIAL_HEADERS = Set.of("authorization", "proxy-authorization", "cookie", "set-cookie");

    private HttpHeaderMasker() {
        // Utility class, no instantiation
    }

    /**
     * Returns the loggable value of a header.
     *
     * @param name           the header name
     * @param value          the raw header value
     * @param passwordMasker the masker holding the configured fields to mask
     * @return {@code ***} for sensitive headers, otherwise the value with embedded passwords masked
     */
    public static String mask(final String name, final String value, final PasswordMasker passwordMasker) {
        return isSensitive(name, passwordMasker) ? MASKED_VALUE : passwordMasker.maskPasswordsIn(value);
    }

    private static boolean isSensitive(final String name, final PasswordMasker passwordMasker) {
        return CREDENTIAL_HEADERS.contains(name.toLowerCase(Locale.ROOT)) || passwordMasker.masksField(name);
    }
}
