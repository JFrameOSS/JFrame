package io.github.jframe.logging.filter;

/**
 * Marker interface for JFrame JAX-RS filters.
 *
 * <p>Implementing this interface identifies a filter as part of the JFrame framework,
 * allowing it to be discovered via CDI {@code Instance&lt;JFrameFilter&gt;} injection
 * and reported in the application startup log.
 *
 * <p>No methods are declared — this is a pure marker contract.
 */
public interface JFrameFilter {
}
