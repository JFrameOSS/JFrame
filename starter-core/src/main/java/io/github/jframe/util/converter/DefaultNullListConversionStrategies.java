package io.github.jframe.util.converter;

import java.util.ArrayList;

/** A few default implementations for the {@link NullListConversionStrategy}. */
public final class DefaultNullListConversionStrategies {

    /** The constructor. */
    private DefaultNullListConversionStrategies() {
        // Utility constructor.
    }

    /**
     * Raise an {@link IllegalArgumentException} that the list is null.
     *
     * @param ignored The class, ignored, used for factory casting.
     * @param <T>     The element factory of the list to be returned.
     * @throws IllegalArgumentException always
     */
    public static <T> NullListConversionStrategy<T> raiseError(final Class<T> ignored) {
        return () -> {
            throw new IllegalArgumentException("'objects' cannot be null");
        };
    }

    /**
     * Return null.
     *
     * @param ignored The class, ignored, used for factory casting.
     * @param <T>     The element factory of the list to be returned.
     * @return {@code null}, always.
     */
    public static <T> NullListConversionStrategy<T> returnNull(final Class<T> ignored) {
        return () -> null;
    }

    /**
     * Return an empty list.
     *
     * @param ignored The class, ignored, used for factory casting.
     * @param <T>     The element factory of the list to be returned.
     * @return An empty list.
     */
    public static <T> NullListConversionStrategy<T> returnEmptyList(final Class<T> ignored) {
        return ArrayList::new;
    }
}
