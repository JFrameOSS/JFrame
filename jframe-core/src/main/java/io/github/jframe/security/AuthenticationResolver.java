package io.github.jframe.security;

/**
 * Framework-agnostic interface for resolving the authenticated subject.
 *
 * <p>Implemented by framework adapters (Spring Security, Quarkus Security) to provide
 * a consistent way to obtain the current authenticated user's identifier across
 * tracing, logging, and auditing components.
 */
@SuppressWarnings("PMD.ImplicitFunctionalInterface")
public interface AuthenticationResolver {

    /**
     * Returns the identifier of the currently authenticated subject.
     *
     * <p>Implementations should return {@link AuthenticationConstants#ANONYMOUS} when
     * no authentication context is present, and {@link AuthenticationConstants#INCOMPLETE}
     * when authentication exists but no name can be resolved.
     *
     * @return the authenticated subject identifier; never {@code null}
     */
    String getAuthenticatedSubject();
}
