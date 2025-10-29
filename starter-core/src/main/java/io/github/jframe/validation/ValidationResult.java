package io.github.jframe.validation;

import io.github.jframe.validation.field.FieldRejection;

import java.io.Serial;
import java.io.Serializable;
import java.util.ArrayDeque;
import java.util.Deque;
import java.util.LinkedList;
import java.util.List;
import java.util.NoSuchElementException;

import org.apache.commons.lang3.StringUtils;
import org.apache.commons.lang3.builder.ReflectionToStringBuilder;
import org.hamcrest.Matcher;

import static java.util.Collections.unmodifiableList;
import static org.apache.commons.lang3.builder.ToStringStyle.SHORT_PREFIX_STYLE;

/**
 * Stores validation errors for a specific object.
 *
 * <p>This class is heavily inspired on Spring's {@link org.springframework.validation.Errors}
 * interface. The main difference is that JFrame's {@link ValidationResult} does not bind or require the target object being validated.
 */
public class ValidationResult implements Serializable {

    /** The serial version UID. */
    @Serial
    private static final long serialVersionUID = 6587801940281589895L;

    /**
     * The separator between path elements in a nested path, for example in "name" or "address.street".
     */
    private static final String NESTED_PATH_SEPARATOR = ".";

    private static final String NESTED_PATH_INDEX_PREFIX = "[";
    private static final String NESTED_PATH_INDEX_SUFFIX = "]";

    private final Deque<String> nestedPathStack = new ArrayDeque<>();
    private final List<ValidationError> errors = new LinkedList<>();

    /**
     * Returns the current nested path of this {@link ValidationResult}.
     *
     * @return the current nested path
     */
    public String getNestedPath() {
        final String nestedPath = this.nestedPathStack.peek();
        return StringUtils.defaultString(nestedPath);
    }

    /** Push the nested path. */
    public void pushNestedPath(final String path) {
        doPushNestedPath(path, null);
    }

    /** Push the nested path. */
    public void pushNestedPath(final String path, final int index) {
        doPushNestedPath(path, index);
    }

    private void doPushNestedPath(final String path, final Integer index) {
        final StringBuilder nestedPathBuilder = new StringBuilder(getNestedPath());
        if (!nestedPathBuilder.isEmpty()) {
            nestedPathBuilder.append(NESTED_PATH_SEPARATOR);
        }
        nestedPathBuilder.append(path);
        if (index != null) {
            nestedPathBuilder
                .append(NESTED_PATH_INDEX_PREFIX)
                .append(index)
                .append(NESTED_PATH_INDEX_SUFFIX);
        }
        this.nestedPathStack.push(nestedPathBuilder.toString());
    }

    /** Remove the nested path. */
    public void popNestedPath() {
        try {
            this.nestedPathStack.pop();
        } catch (final NoSuchElementException exception) {
            throw new IllegalStateException("Cannot pop nested path: no nested path on stack", exception);
        }
    }

    /**
     * Returns {@code true} if this validation result contains errors.
     *
     * @return {@code true} if this validation result contains errors
     */
    public boolean hasErrors() {
        return !this.errors.isEmpty();
    }

    /**
     * Returns the validation errors.
     *
     * @return the validation errors
     */
    public List<ValidationError> getErrors() {
        return unmodifiableList(this.errors);
    }

    /** Reject the value. */
    public void reject(final String code) {
        addError(new ValidationError(code));
    }

    /** Reject the value {@code expr} is {@code true}. */
    public void rejectIf(final boolean expr, final String code) {
        if (expr) {
            reject(code);
        }
    }

    /** Reject the value {@code actual} if the {@code matcher} matches. */
    public <T> void rejectIf(final T actual, final Matcher<? super T> matcher, final String code) {
        rejectIf(matcher.matches(actual), code);
    }

    /** Reject the value with {@code code} error. */
    public void rejectValue(final String code) {
        rejectValue(null, code);
    }

    /** Reject the value for {@code field} with {@code code} error. */
    public void rejectValue(final String field, final String code) {
        final StringBuilder fieldBuilder = new StringBuilder(getNestedPath());
        if (StringUtils.isNotBlank(field)) {
            if (!fieldBuilder.isEmpty()) {
                fieldBuilder.append(NESTED_PATH_SEPARATOR);
            }
            fieldBuilder.append(field);
        }
        if (fieldBuilder.isEmpty()) {
            reject(code);
        } else {
            addError(new ValidationError(fieldBuilder.toString(), code));
        }
    }

    /** Reject the value if {@code expr} is {@code true}. Will return the {@code code} error. */
    public void rejectValueIf(final boolean expr, final String code) {
        if (expr) {
            rejectValue(code);
        }
    }

    /**
     * Reject the value if {@code matcher} matches the value {@code actual}. Will return the {@code code} error on the {@code field}.
     */
    public <T> void rejectValueIf(final T actual, final Matcher<? super T> matcher, final String code) {
        rejectValueIf(matcher.matches(actual), code);
    }

    /**
     * Reject the value if {@code expr} is {@code true}. Will return the {@code code} error on the {@code field}.
     */
    public void rejectValueIf(final boolean expr, final String field, final String code) {
        if (expr) {
            rejectValue(field, code);
        }
    }

    /**
     * Reject the value if the value matches the {@code matcher}. Will return the {@code code} error on the {@code field}.
     */
    public <T> void rejectValueIf(final T actual, final Matcher<? super T> matcher, final String field, final String code) {
        rejectValueIf(matcher.matches(actual), field, code);
    }

    /**
     * Reject a <code>field</code> with value <code>actual</code> in a fluent manner.
     *
     * <p>For instance:
     *
     * <pre>
     * validationResult.rejectField("houseNumber", "13-a")
     * .whenNull()
     * .orWhen(containsString("a"))
     * .orWhen(h -&gt; h.length() &gt; 4);
     * </pre>
     *
     * @param field  The field name to evaluate.
     * @param actual The value to evaluate.
     * @param <T>    The factory of the value.
     * @return a new field rejection.
     */
    public <T> FieldRejection<T> rejectField(final String field, final T actual) {
        return new FieldRejection<>(this, field, actual);
    }

    /**
     * Adds the supplied {@link ValidationError} to this {@link ValidationResult}.
     *
     * @param error the validation error
     */
    public void addError(final ValidationError error) {
        this.errors.add(error);
    }

    /**
     * Adds the supplied {@link ValidationError}s to this {@link ValidationResult}.
     *
     * @param errors the validation errors
     */
    public void addAllErrors(final List<ValidationError> errors) {
        this.errors.addAll(errors);
    }

    /**
     * Adds all errors from the supplied {@link ValidationResult} to this {@link ValidationResult}.
     *
     * @param validationResult the validation result to merge in
     */
    public void addAllErrors(final ValidationResult validationResult) {
        this.errors.addAll(validationResult.getErrors());
    }

    @Override
    public String toString() {
        return ReflectionToStringBuilder.toString(this, SHORT_PREFIX_STYLE);
    }
}
