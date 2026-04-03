package io.github.jframe.tracing;

/**
 * Utility for generating consistent OpenTelemetry span names.
 *
 * <p>Handles proxy class name stripping (Quarkus CDI {@code _Subclass} suffix
 * and Spring CGLIB {@code $} suffix) and span name resolution.
 */
public final class SpanNamingUtil {

    private static final String SUBCLASS_SUFFIX = "_Subclass";

    private SpanNamingUtil() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Strips framework proxy suffixes from a simple class name.
     *
     * <p>Removes the Quarkus CDI {@code _Subclass} suffix and the Spring CGLIB
     * {@code $...} portion, returning the original class name.
     *
     * @param simpleClassName the {@code Class#getSimpleName()} of the target class
     * @return the clean class name without proxy suffixes
     */
    public static String resolveClassName(final String simpleClassName) {
        String name = simpleClassName;
        final int dollarIndex = name.indexOf('$');
        if (dollarIndex > 0) {
            name = name.substring(0, dollarIndex);
        }
        if (name.endsWith(SUBCLASS_SUFFIX)) {
            name = name.substring(0, name.length() - SUBCLASS_SUFFIX.length());
        }
        return name;
    }

    /**
     * Resolves the span name to use for a traced method invocation.
     *
     * <p>Returns {@code customName} when it is non-null and non-empty; otherwise
     * falls back to {@code "ClassName.methodName"}.
     *
     * @param className  the resolved (proxy-stripped) class name
     * @param methodName the method name
     * @param customName an optional custom span name (may be {@code null} or empty)
     * @return the span name to use
     */
    public static String resolveSpanName(final String className, final String methodName, final String customName) {
        if (customName != null && !customName.isEmpty()) {
            return customName;
        }
        return className + "." + methodName;
    }
}
