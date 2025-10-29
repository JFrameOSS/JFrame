package io.github.jframe.validation.field;

import io.github.jframe.validation.ValidationResult;

import java.util.function.Function;
import java.util.function.Predicate;

import org.hamcrest.Matcher;

import static org.hamcrest.Matchers.nullValue;

/**
 * Reject a field based on a few conditions.
 *
 * <p>For instance:
 *
 * <pre>
 * new FieldRejection(validationResult, "houseNumber", "13-a")
 * .whenNull()
 * .orWhen(h -&gt; h.contains("a"))
 * .orWhen(h -&gt; h.length() &gt; 10);
 * </pre>
 *
 * <p>If used with the ValidationResult class this will look like:
 *
 * <pre>
 * validationResult.rejectField("houseNumber", "13-a")
 * .whenNull()
 * .orWhen(h -&gt; h.contains("a'))
 * .orWhen(h -&gt; h.length() &gt; 10);
 * </pre>
 *
 * <p>The rejections without <code>code</code> parameters have the value <code>invalid</code>,
 * except the <code>whenNull()</code>, this has the <code>required</code> code value.
 *
 * <p>The chain will stop evaluating the rejection clauses after the first matching clause. In the
 * examples above the chain will not evaluate the length of the house number.
 *
 * @param <T> The factory of the value to evaluate.
 */
@SuppressWarnings("PMD.TooManyMethods")
public class FieldRejection<T> {

    public static final String REQUIRED = "required";
    public static final String INVALID = "invalid";
    private final ValidationResult validationResult;
    private final String field;
    private final T actual;

    private boolean mustEvaluate = true;

    /**
     * Construct a new field rejection.
     *
     * @param validationResult The validation result.
     * @param field            The field name (property name).
     * @param actual           The field's value.
     */
    public FieldRejection(final ValidationResult validationResult, final String field, final T actual) {
        this.validationResult = validationResult;
        this.field = field;
        this.actual = actual;
    }

    private FieldRejection<T> evaluate(final Matcher<?> matcher, final String code) {
        if (mustEvaluate && matcher.matches(actual)) {
            mustEvaluate = false;
            validationResult.rejectValue(field, code);
        }

        return this;
    }

    /**
     * Syntactic sugar, allows the use of <code>or().when(...).or().when(...)</code> syntaxis.
     *
     * @return The current field rejection.
     */
    public FieldRejection<T> or() {
        return this;
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with the error
     * code
     * <code>invalid</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> or(final Matcher<T> matcher) {
        return when(matcher);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with supplied the
     * error code <code>code</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> or(final Matcher<T> matcher, final String code) {
        return when(matcher, code);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with the error code
     * <code>invalid</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> or(final Predicate<T> predicate) {
        return when(predicate);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with supplied the error
     * code <code>code</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> or(final Predicate<T> predicate, final String code) {
        return when(predicate, code);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with the
     * error code <code>invalid</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> or(final Function<T, R> function, final Matcher<R> matcher) {
        return when(function, matcher, INVALID);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with
     * supplied the error code <code>code</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> or(final Function<T, R> function, final Matcher<R> matcher, final String code) {
        return when(function, matcher, code);
    }

    /**
     * Rejects the field when the <code>actual</code> is <code>null</code>. This will reject the field with the error code
     * <code>required</code>.
     *
     * @return The current field rejection.
     */
    public FieldRejection<T> whenNull() {
        return whenNull(REQUIRED);
    }

    /**
     * Rejects the field when the <code>actual</code> is <code>null</code>. This will reject the field with supplied the error code
     * <code>code</code>.
     *
     * @param code The error code to set if the actual value is <code>null</code>.
     * @return The current field rejection.
     */
    public FieldRejection<T> whenNull(final String code) {
        return evaluate(nullValue(), code);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with the error
     * code
     * <code>invalid</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> when(final Matcher<T> matcher) {
        return when(matcher, INVALID);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with supplied the
     * error code <code>code</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> when(final Matcher<T> matcher, final String code) {
        return evaluate(matcher, code);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with the error code
     * <code>invalid</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> when(final Predicate<T> predicate) {
        return when(predicate, INVALID);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with the
     * error code <code>invalid</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> when(final Function<T, R> function, final Matcher<R> matcher) {
        return when(function, matcher, INVALID);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with
     * supplied the error code <code>code</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> when(final Function<T, R> function, final Matcher<R> matcher, final String code) {
        return when(value -> matcher.matches(function.apply(value)), code);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with supplied the error
     * code <code>code</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> when(final Predicate<T> predicate, final String code) {
        if (mustEvaluate && predicate.test(actual)) {
            mustEvaluate = false;
            validationResult.rejectValue(field, code);
        }

        return this;
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with the error
     * code
     * <code>invalid</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> orWhen(final Matcher<T> matcher) {
        return when(matcher);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the <code>actual</code> value. This will reject the field with supplied the
     * error code <code>code</code>.
     *
     * @param matcher The matcher to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> orWhen(final Matcher<T> matcher, final String code) {
        return when(matcher, code);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with the error code
     * <code>invalid</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> orWhen(final Predicate<T> predicate) {
        return when(predicate);
    }

    /**
     * Rejects the field when the <code>predicate</code> evaluates to <code>true</code>. This will reject the field with supplied the error
     * code <code>code</code>.
     *
     * @param predicate The predicate to use.
     * @return The current field rejection.
     */
    public FieldRejection<T> orWhen(final Predicate<T> predicate, final String code) {
        return when(predicate, code);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with the
     * error code <code>invalid</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> orWhen(final Function<T, R> function, final Matcher<R> matcher) {
        return when(function, matcher, INVALID);
    }

    /**
     * Rejects the field when the <code>matcher</code> matches the result of the <code>function</code> . This will reject the field with
     * supplied the error code <code>code</code>.
     *
     * @param function The function to apply to the <code>actual</code> value.
     * @param matcher  The matcher to use against the return value of the <code>function</code>.
     * @return The current field rejection.
     */
    public <R> FieldRejection<T> orWhen(final Function<T, R> function, final Matcher<R> matcher, final String code) {
        return when(function, matcher, code);
    }
}
