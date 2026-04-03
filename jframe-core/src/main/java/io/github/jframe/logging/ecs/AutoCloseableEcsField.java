package io.github.jframe.logging.ecs;


import java.util.Collection;

import static io.github.jframe.logging.ecs.EcsFields.tagCloseable;


/** A wrapper around a multiEcsField where the field is closeable. */
public interface AutoCloseableEcsField extends EcsField, AutoCloseable {

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableEcsField and(final EcsField field, final Enum<?> value) {
        final AutoCloseableEcsField other = tagCloseable(field, value);
        return new CompoundAutoCloseableEcsField(this, other);
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableEcsField and(final EcsField field, final int value) {
        final AutoCloseableEcsField other = tagCloseable(field, value);
        return new CompoundAutoCloseableEcsField(this, other);
    }

    /**
     * Chains another {@link EcsField} with a {@code long} value.
     *
     * @param field the ECS log field
     * @param value the long value
     * @return the chained auto-closeable field
     */
    default AutoCloseableEcsField and(final EcsField field, final long value) {
        return and(field, String.valueOf(value));
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableEcsField and(final EcsField field, final String value) {
        final AutoCloseableEcsField other = tagCloseable(field, value);
        return new CompoundAutoCloseableEcsField(this, other);
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableEcsField and(final EcsField field, final Collection<String> value) {
        final AutoCloseableEcsField other = tagCloseable(field, value);
        return new CompoundAutoCloseableEcsField(this, other);
    }

    @Override
    void close();
}
