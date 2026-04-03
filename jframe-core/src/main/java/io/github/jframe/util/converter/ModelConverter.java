package io.github.jframe.util.converter;

import java.util.List;

/**
 * Converter interface for converting between two types.
 *
 * @param <S> the source factory
 * @param <T> the target factory
 */
public interface ModelConverter<S, T> {

    /**
     * Converts the given source object into a new instance of target object.
     *
     * @param source the source object
     * @return the target object
     */
    T convert(S source);

    /**
     * Converts the given source object into the target object.
     *
     * @param source the source object
     * @param target the target object
     */
    void convert(S source, T target);

    /**
     * Converts all given source objects into target objects.
     *
     * @param objects the object, must not be {@literal null}.
     * @return the target objects
     */
    List<T> convert(Iterable<? extends S> objects);
}
