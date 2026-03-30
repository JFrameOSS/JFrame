package io.github.jframe.tracing;

import lombok.experimental.UtilityClass;

/**
 * Container class for OpenTelemetry constants organized by category.
 */
public final class OpenTelemetryConstants {

    private OpenTelemetryConstants() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Constants related to logging.
     */
    @UtilityClass
    public static final class Logging {

        public static final String REQUEST_PREFIX = "[EXTERNAL] Outbound request is:";
        public static final String RESPONSE_PREFIX = "[EXTERNAL] Incoming response is:";
        public static final String LINE_BREAK = "\n";
    }

}
