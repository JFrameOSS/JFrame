package io.github.jframe.tracing;

import java.util.Set;

/**
 * Rules for excluding methods from tracing instrumentation.
 *
 * <p>Provides a constant set and a utility method to determine whether a method
 * should be excluded from span creation, based on well-known Java object methods and application-specific configuration.
 */
public final class MethodExclusionRules {

    /** Well-known Java object methods that are always excluded. */
    public static final Set<String> EXCLUDED_NAMES = Set.of("toString", "hashCode", "equals", "clone");

    private MethodExclusionRules() {
        throw new UnsupportedOperationException("Utility class");
    }

    /**
     * Returns {@code true} if the given method should be excluded from tracing.
     *
     * <p>A method is excluded when:
     * <ul>
     * <li>its name is one of {@link #EXCLUDED_NAMES}, or</li>
     * <li>its lowercase name is present in {@code configExcludedMethods}.</li>
     * </ul>
     *
     * @param methodName            the simple method name to test
     * @param configExcludedMethods set of method names (lowercase) from application configuration
     * @return {@code true} if the method should be excluded
     */
    public static boolean isExcluded(final String methodName, final Set<String> configExcludedMethods) {
        return EXCLUDED_NAMES.contains(methodName) || configExcludedMethods.contains(methodName.toLowerCase());
    }
}
