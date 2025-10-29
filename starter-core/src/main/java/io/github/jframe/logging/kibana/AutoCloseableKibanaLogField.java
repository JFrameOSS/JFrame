package io.github.jframe.logging.kibana;


import java.util.Collection;

import static io.github.jframe.logging.kibana.KibanaLogFields.tagCloseable;


/** A wrapper around a multiKibanaLogField where the field is closeable. */
public interface AutoCloseableKibanaLogField extends KibanaLogField, AutoCloseable {

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableKibanaLogField and(final KibanaLogField field, final Enum<?> value) {
        final AutoCloseableKibanaLogField other = tagCloseable(field, value);
        return new CompoundAutocloseableKibanaLogField(this, other);
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableKibanaLogField and(final KibanaLogField field, final int value) {
        final AutoCloseableKibanaLogField other = tagCloseable(field, value);
        return new CompoundAutocloseableKibanaLogField(this, other);
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableKibanaLogField and(final KibanaLogField field, final String value) {
        final AutoCloseableKibanaLogField other = tagCloseable(field, value);
        return new CompoundAutocloseableKibanaLogField(this, other);
    }

    /**
     * Chain the closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    default AutoCloseableKibanaLogField and(final KibanaLogField field, final Collection<String> value) {
        final AutoCloseableKibanaLogField other = tagCloseable(field, value);
        return new CompoundAutocloseableKibanaLogField(this, other);
    }

    @Override
    void close();
}
