package io.github.jframe.tracing;

import java.util.Arrays;
import java.util.Set;
import java.util.stream.Collectors;

/**
 * Shared configuration defaults and utility constants for OTLP configuration.
 *
 * <p>Provides constants for default values used across Spring and Quarkus OTLP modules,
 * plus a utility method for parsing comma-separated configuration strings.
 */
public final class OtlpDefaults {

    /** Configuration property prefix for OTLP settings. */
    public static final String PREFIX = "jframe.otlp.";

    /** Default disabled flag — OTLP tracing is enabled by default. */
    public static final boolean DEFAULT_DISABLED = false;

    /** Default OTLP collector endpoint URL. */
    public static final String DEFAULT_URL = "http://localhost:4318";

    /** Default timeout for OTLP export requests. */
    public static final String DEFAULT_TIMEOUT = "10s";

    /** Default exporter type. */
    public static final String DEFAULT_EXPORTER = "otlp";

    /** Default sampling rate — 100% sampling. */
    public static final double DEFAULT_SAMPLING_RATE = 1.0;

    /** Default HTTP methods excluded from tracing. */
    public static final String DEFAULT_EXCLUDED_METHODS = "health,actuator,ping,status,info,metrics";

    /** Default W3C trace context propagators. */
    public static final String DEFAULT_PROPAGATORS = "tracecontext,baggage";

    private OtlpDefaults() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Parses a comma-separated string into an unmodifiable {@link Set} of trimmed, non-empty values.
     *
     * @param csv the comma-separated input string
     * @return an unmodifiable set containing the parsed values
     */
    public static Set<String> parseCommaSeparated(final String csv) {
        return Arrays.stream(csv.split(","))
            .map(String::trim)
            .filter(s -> !s.isEmpty())
            .collect(Collectors.toUnmodifiableSet());
    }
}
