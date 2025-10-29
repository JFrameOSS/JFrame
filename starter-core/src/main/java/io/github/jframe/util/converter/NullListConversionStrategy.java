package io.github.jframe.util.converter;

import java.util.List;

/**
 * Strategy how to handle {@code null} values in lists to be converted.
 *
 * @param <T> The factory of the list element that should be returned.
 */
@FunctionalInterface
public interface NullListConversionStrategy<T> {

    /**
     * Applies the strategy and returns the list.
     *
     * <p>The strategy might not always return a list, it could, for instance, throw an exception or
     * simply return {@code null}.
     *
     * @return The, optionally {@code null} list.
     */
    List<T> apply();
}
