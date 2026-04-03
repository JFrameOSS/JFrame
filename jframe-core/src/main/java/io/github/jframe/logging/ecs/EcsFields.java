package io.github.jframe.logging.ecs;

import lombok.experimental.UtilityClass;

import java.util.Collection;

import org.apache.commons.lang3.StringUtils;
import org.slf4j.MDC;

import static java.util.Objects.nonNull;
import static java.util.stream.Collectors.joining;
import static org.apache.commons.lang3.StringUtils.isBlank;

/**
 * Class that holds the extra fields used for ECS logging.
 *
 * <p>Log lines for structured will contain all fields set, until the log fields are cleared by invoking
 * {@link EcsFields#clear()}.
 */
@UtilityClass
public class EcsFields {

    /**
     * Sets the ECS log field {@code field} to the {@code value}.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return The field, set.
     */
    public static EcsField tag(final EcsField field, final Enum<?> value) {
        return tag(field, value.toString());
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return The field, set.
     */
    public static EcsField tag(final EcsField field, final int value) {
        return tag(field, Integer.toString(value));
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}.
     *
     * @param field the ECS log field
     * @param value the long value
     * @return the tagged field
     */
    public static EcsField tag(final EcsField field, final long value) {
        return tag(field, String.valueOf(value));
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return The field, set.
     */
    public static EcsField tag(final EcsField field, final String value) {
        if (isBlank(value)) {
            clear(field);
            return field;
        }
        MDC.put(field.getKey(), value);
        return field;
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}.
     *
     * @param field  The field to set.
     * @param values The values to set.
     * @return The field, set.
     */
    public static EcsField tag(final EcsField field, final Collection<String> values) {
        if (values == null || values.isEmpty()) {
            return tag(field, (String) null);
        }

        final String value = values.stream()
            .map(item -> String.format("'%s'", item))
            .collect(joining(", ", "[", "]"));
        return tag(field, value);
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}, returns an auto closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    public static AutoCloseableEcsField tagCloseable(final EcsField field, final Enum<?> value) {
        return tagCloseable(field, value.toString());
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}, returns an auto closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    public static AutoCloseableEcsField tagCloseable(final EcsField field, final int value) {
        return tagCloseable(field, Integer.toString(value));
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}, returns an auto closeable.
     *
     * @param field the ECS log field
     * @param value the long value
     * @return the auto-closeable tagged field
     */
    public static AutoCloseableEcsField tagCloseable(final EcsField field, final long value) {
        return tagCloseable(field, String.valueOf(value));
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}, returns an auto closeable.
     *
     * @param field The field to set.
     * @param value The value to set.
     * @return a closable field.
     */
    public static AutoCloseableEcsField tagCloseable(final EcsField field, final String value) {
        return new AutoCloseableEcsFieldImpl(tag(field, value));
    }

    /**
     * Sets the ECS log field {@code field} to the {@code value}, returns an auto closeable.
     *
     * @param field  The field to set.
     * @param values The value to set.
     * @return a closable field.
     */
    public static AutoCloseableEcsField tagCloseable(
        final EcsField field, final Collection<String> values) {
        return new AutoCloseableEcsFieldImpl(tag(field, values));
    }

    /**
     * Retrieves the value for the {@code field}. It will return {@code null} if no value is set.
     *
     * @param field The field to retrieve the value for.
     * @return The fields value, possibly {@code null}.
     */
    public static String get(final EcsField field) {
        return getOrDefault(field, null);
    }

    /**
     * Retrieves the value for the {@code field}. It will return {@code defaultValue} if no value is set.
     *
     * @param field        The field to retrieve the value for.
     * @param defaultValue The value if there is no value set.
     * @return The field's value or the default.
     */
    public static String getOrDefault(final EcsField field, final String defaultValue) {
        return StringUtils.defaultIfEmpty(MDC.get(field.getKey()), defaultValue);
    }

    /**
     * Removes the value for the field {@code field}.
     *
     * @param field The field to remove.
     */
    public static void clear(final EcsField field) {
        MDC.remove(field.getKey());
    }

    /**
     * Removes the value for the fields {@code fields}.
     *
     * @param fields The fields to remove the values for.
     */
    public static void clear(final EcsField... fields) {
        for (final EcsField field : fields) {
            MDC.remove(field.getKey());
        }
    }

    /** Removes all values set for all fields. */
    public static void clear() {
        MDC.clear();
    }

    /**
     * Getter for the log string.
     *
     * @return the log string
     */
    public static String getValuesAsLogString() {
        final StringBuilder result = new StringBuilder();
        final java.util.Map<String, String> contextMap = MDC.getCopyOfContextMap();
        if (contextMap != null) {
            contextMap.forEach(
                (key, value) -> {
                    if (!key.equals(EcsFieldNames.LOG_TYPE.getKey())) {
                        result.append(' ').append(key).append("=\"").append(value).append('"');
                    }
                }
            );
        }

        return result.toString();
    }

    /**
     * Update log fields based on the {@code EcsLogContext}.
     *
     * <p>See {@link EcsFields#getMdcContext()}.
     *
     * @param logContext The context to copy.
     */
    public static void populateFromContext(final MdcLogContext logContext) {
        if (nonNull(logContext)) {
            MDC.setContextMap(logContext.getContextMap());
        }
    }

    /**
     * Create a new log context for the current thread's ECS log fields.
     *
     * <p>See {@link MdcLogContext#registerEcsFieldsInThisThread()}.
     *
     * @return a log context to use in another thread.
     */
    public static MdcLogContext getMdcContext() {
        return new MdcLogContext();
    }
}
