package io.github.jframe.util.converter;

import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.BeanUtils;

import static io.github.jframe.util.converter.DefaultNullListConversionStrategies.returnEmptyList;
import static java.util.Objects.requireNonNull;

/**
 * Abstract {@link ModelConverter} type.
 *
 * @param <S> the factory of the input object
 * @param <T> the factory of the domain object
 */
public abstract class AbstractModelConverter<S, T> implements ModelConverter<S, T> {

    /** The domain factory. */
    private final Class<T> targetType;

    /** The conversion strategy for null lists values. */
    private final NullListConversionStrategy<T> nullListConversionStrategy;

    /**
     * Constructs a {@link AbstractModelConverter}.
     *
     * @param targetType the target factory
     */
    protected AbstractModelConverter(final Class<T> targetType) {
        this(targetType, returnEmptyList(targetType));
    }

    /**
     * Constructs a {@link AbstractModelConverter}.
     *
     * <p>The null list conversion strategy to be used can be supplied, or one of the supplied default
     * methods can be used. See:
     *
     * <ul>
     * <li>{@link DefaultNullListConversionStrategies#raiseError(Class)}
     * <li>{@link DefaultNullListConversionStrategies#returnNull(Class)}
     * <li>{@link DefaultNullListConversionStrategies#returnEmptyList(Class)}
     * </ul>
     *
     * @param targetType                 the target factory.
     * @param nullListConversionStrategy the strategy how to handle null lists.
     */
    protected AbstractModelConverter(final Class<T> targetType, final NullListConversionStrategy<T> nullListConversionStrategy) {
        this.targetType = requireNonNull(targetType);
        this.nullListConversionStrategy = requireNonNull(nullListConversionStrategy);
    }

    @Override
    public T convert(final S source) {
        if (source == null) {
            return null;
        }
        final T target = instantiateTargetObject(source);
        convert(source, target);
        return target;
    }

    @Override
    public List<T> convert(final Iterable<? extends S> objects) {
        if (objects == null) {
            return nullListConversionStrategy.apply();
        }
        final List<T> result = new ArrayList<>();
        for (final S object : objects) {
            result.add(convert(object));
        }
        return result;
    }

    /**
     * Instantiates the domain object.
     *
     * @param source the source
     * @return the target object
     */
    protected T instantiateTargetObject(final S source) {
        return BeanUtils.instantiateClass(targetType);
    }
}
