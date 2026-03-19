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
     * Chains another {@link KibanaLogField} with a {@code long} value.
     *
     * @param field the Kibana log field
     * @param value the long value
     * @return the chained auto-closeable field
     */
    default AutoCloseableKibanaLogField and(final KibanaLogField field, final long value) {
        return and(field, String.valueOf(value));
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
