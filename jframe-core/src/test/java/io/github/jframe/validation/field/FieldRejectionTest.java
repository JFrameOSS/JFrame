package io.github.jframe.validation.field;

import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.greaterThan;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.lessThan;

/**
 * Tests for {@link FieldRejection}.
 *
 * <p>Verifies the FieldRejection fluent API functionality including:
 * <ul>
 * <li>Null value rejection with default and custom error codes</li>
 * <li>Hamcrest matcher-based rejection</li>
 * <li>Predicate-based rejection</li>
 * <li>Function-based rejection with matchers</li>
 * <li>Chaining with or() and orWhen() syntactic sugar</li>
 * <li>Short-circuit evaluation (stops after first match)</li>
 * <li>Integration with ValidationResult</li>
 * <li>Default error codes (REQUIRED, INVALID)</li>
 * </ul>
 */
@DisplayName("Validation Framework - Field Rejection")
public class FieldRejectionTest extends UnitTest {

    private ValidationResult validationResult;

    @BeforeEach
    @Override
    public void setUp() {
        super.setUp();
        // Given: A fresh validation result for each test
        validationResult = new ValidationResult();
    }

    @Test
    @DisplayName("Should reject null value with default REQUIRED code")
    public void shouldRejectNullWithDefaultCode() {
        // Given: A field rejection with null value
        final String field = "email";
        final String value = null;

        // When: Rejecting when null with default code
        validationResult.rejectField(field, value).whenNull();

        // Then: Error is added with REQUIRED code
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getField(), is(equalTo(field)));
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(FieldRejection.REQUIRED)));
    }

    @Test
    @DisplayName("Should reject null value with custom code")
    public void shouldRejectNullWithCustomCode() {
        // Given: A field rejection with null value and custom code
        final String field = "username";
        final String value = null;
        final String customCode = "CUSTOM_REQUIRED";

        // When: Rejecting when null with custom code
        validationResult.rejectField(field, value).whenNull(customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should not reject non-null value with whenNull")
    public void shouldNotRejectNonNullValue() {
        // Given: A field rejection with non-null value
        final String value = "valid@email.com";

        // When: Rejecting when null
        validationResult.rejectField("email", value).whenNull();

        // Then: No error is added
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should reject when matcher matches with default INVALID code")
    public void shouldRejectWhenMatcherMatches() {
        // Given: A field rejection with value that matches the matcher
        final String value = "test123";

        // When: Rejecting when matcher matches
        validationResult.rejectField("field", value).when(containsString("123"));

        // Then: Error is added with INVALID code
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(FieldRejection.INVALID)));
    }

    @Test
    @DisplayName("Should reject when matcher matches with custom code")
    public void shouldRejectWhenMatcherMatchesWithCustomCode() {
        // Given: A field rejection with value and custom code
        final String value = "test";
        final String customCode = "CONTAINS_TEST";

        // When: Rejecting when matcher matches with custom code
        validationResult.rejectField("field", value).when(containsString("test"), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should not reject when matcher does not match")
    public void shouldNotRejectWhenMatcherDoesNotMatch() {
        // Given: A field rejection with value that does not match
        final String value = "test";

        // When: Rejecting when matcher matches
        validationResult.rejectField("field", value).when(containsString("xyz"));

        // Then: No error is added
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should reject when predicate evaluates to true")
    public void shouldRejectWhenPredicateIsTrue() {
        // Given: A field rejection with value and a predicate that returns true
        final String value = "short";

        // When: Rejecting when predicate is true
        validationResult.rejectField("field", value).when(v -> v.length() < 10);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should reject when predicate evaluates to true with custom code")
    public void shouldRejectWhenPredicateIsTrueWithCustomCode() {
        // Given: A field rejection with value and custom code
        final Integer value = 150;
        final String customCode = "TOO_HIGH";

        // When: Rejecting when predicate is true with custom code
        validationResult.rejectField("age", value).when(v -> v > 120, customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should not reject when predicate evaluates to false")
    public void shouldNotRejectWhenPredicateIsFalse() {
        // Given: A field rejection with value and a predicate that returns false
        final Integer value = 25;

        // When: Rejecting when predicate is true
        validationResult.rejectField("age", value).when(v -> v > 120);

        // Then: No error is added
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should reject when function result matches matcher")
    public void shouldRejectWhenFunctionResultMatches() {
        // Given: A field rejection with value and a function with matcher
        final String value = "Test String";

        // When: Rejecting when function result matches
        validationResult.rejectField("field", value).when(String::length, greaterThan(20));

        // Then: No error is added because length is not greater than 20
        assertThat(validationResult.hasErrors(), is(false));

        // When: Using a matching condition
        validationResult.rejectField("field", value).when(String::length, lessThan(20));

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should reject when function result matches matcher with custom code")
    public void shouldRejectWhenFunctionResultMatchesWithCustomCode() {
        // Given: A field rejection with value, function, matcher, and custom code
        final String value = "AB";
        final String customCode = "TOO_SHORT";

        // When: Rejecting when function result matches with custom code
        validationResult.rejectField("code", value).when(String::length, lessThan(3), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support or syntactic sugar with matcher")
    public void shouldSupportOrSyntacticSugar() {
        // Given: A field rejection with value
        final String value = "test";

        // When: Using or() syntactic sugar
        validationResult.rejectField("field", value)
            .when(containsString("xyz"))
            .or(containsString("test"));

        // Then: Error is added because second condition matches
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support or with matcher and custom code")
    public void shouldSupportOrWithMatcherAndCustomCode() {
        // Given: A field rejection with value
        final String value = "test";
        final String customCode = "CONTAINS_TEST";

        // When: Using or() with custom code
        validationResult.rejectField("field", value)
            .when(containsString("xyz"))
            .or(containsString("test"), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support or with predicate")
    public void shouldSupportOrWithPredicate() {
        // Given: A field rejection with value
        final Integer value = 5;

        // When: Using or() with predicate
        validationResult.rejectField("number", value)
            .when(v -> v > 10)
            .or(v -> v < 10);

        // Then: Error is added because second predicate is true
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support or with predicate and custom code")
    public void shouldSupportOrWithPredicateAndCustomCode() {
        // Given: A field rejection with value
        final Integer value = 5;
        final String customCode = "TOO_SMALL";

        // When: Using or() with predicate and custom code
        validationResult.rejectField("number", value)
            .when(v -> v > 10)
            .or(v -> v < 10, customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support or with function and matcher")
    public void shouldSupportOrWithFunctionAndMatcher() {
        // Given: A field rejection with value
        final String value = "test";

        // When: Using or() with function and matcher
        validationResult.rejectField("field", value)
            .when(String::length, greaterThan(10))
            .or(String::length, lessThan(10));

        // Then: Error is added because second condition matches
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support or with function, matcher and custom code")
    public void shouldSupportOrWithFunctionMatcherAndCustomCode() {
        // Given: A field rejection with value
        final String value = "AB";
        final String customCode = "INVALID_LENGTH";

        // When: Using or() with function, matcher and custom code
        validationResult.rejectField("code", value)
            .when(String::length, greaterThan(10))
            .or(String::length, lessThan(3), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support orWhen with matcher")
    public void shouldSupportOrWhenWithMatcher() {
        // Given: A field rejection with value
        final String value = "test";

        // When: Using orWhen() with matcher
        validationResult.rejectField("field", value)
            .when(containsString("xyz"))
            .orWhen(containsString("test"));

        // Then: Error is added because second condition matches
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support orWhen with matcher and custom code")
    public void shouldSupportOrWhenWithMatcherAndCustomCode() {
        // Given: A field rejection with value
        final String value = "test";
        final String customCode = "FOUND_TEST";

        // When: Using orWhen() with matcher and custom code
        validationResult.rejectField("field", value)
            .when(containsString("xyz"))
            .orWhen(containsString("test"), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support orWhen with predicate")
    public void shouldSupportOrWhenWithPredicate() {
        // Given: A field rejection with value
        final Integer value = 5;

        // When: Using orWhen() with predicate
        validationResult.rejectField("number", value)
            .when(v -> v > 10)
            .orWhen(v -> v < 10);

        // Then: Error is added because second predicate is true
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support orWhen with predicate and custom code")
    public void shouldSupportOrWhenWithPredicateAndCustomCode() {
        // Given: A field rejection with value
        final Integer value = 5;
        final String customCode = "OUT_OF_RANGE";

        // When: Using orWhen() with predicate and custom code
        validationResult.rejectField("number", value)
            .when(v -> v > 10)
            .orWhen(v -> v < 10, customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should support orWhen with function and matcher")
    public void shouldSupportOrWhenWithFunctionAndMatcher() {
        // Given: A field rejection with value
        final String value = "test";

        // When: Using orWhen() with function and matcher
        validationResult.rejectField("field", value)
            .when(String::length, greaterThan(10))
            .orWhen(String::length, lessThan(10));

        // Then: Error is added because second condition matches
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should support orWhen with function, matcher and custom code")
    public void shouldSupportOrWhenWithFunctionMatcherAndCustomCode() {
        // Given: A field rejection with value
        final String value = "AB";
        final String customCode = "LENGTH_INVALID";

        // When: Using orWhen() with function, matcher and custom code
        validationResult.rejectField("code", value)
            .when(String::length, greaterThan(10))
            .orWhen(String::length, lessThan(3), customCode);

        // Then: Error is added with custom code
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(customCode)));
    }

    @Test
    @DisplayName("Should stop evaluation after first match (short-circuit)")
    public void shouldShortCircuitAfterFirstMatch() {
        // Given: A field rejection with value and multiple conditions
        final String value = "test";

        // When: Chaining multiple conditions where first one matches
        validationResult.rejectField("field", value)
            .when(containsString("test"), "FIRST_CODE")
            .orWhen(containsString("test"), "SECOND_CODE")
            .orWhen(containsString("test"), "THIRD_CODE");

        // Then: Only one error is added with the first matching code
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo("FIRST_CODE")));
    }

    @Test
    @DisplayName("Should support complex fluent chain with mixed conditions")
    public void shouldSupportComplexFluentChain() {
        // Given: A field rejection with value
        final String value = "test@example.com";

        // When: Building complex validation chain
        validationResult.rejectField("email", value)
            .whenNull("REQUIRED")
            .orWhen(String::isEmpty, "EMPTY")
            .orWhen(v -> !v.contains("@"), "INVALID_FORMAT")
            .orWhen(String::length, greaterThan(50), "TOO_LONG");

        // Then: No error is added because none of the conditions match
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should integrate with ValidationResult nested paths")
    public void shouldIntegrateWithNestedPaths() {
        // Given: A validation result with nested path
        validationResult.pushNestedPath("user");
        final String value = null;

        // When: Using field rejection
        validationResult.rejectField("email", value).whenNull();

        // Then: Error includes nested path
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getField(), is(equalTo("user.email")));
    }

    @Test
    @DisplayName("Should support or method without parameters for syntactic sugar")
    public void shouldSupportOrMethodWithoutParameters() {
        // Given: A field rejection with value
        final String value = "test";

        // When: Using or() without parameters between conditions
        validationResult.rejectField("field", value)
            .when(containsString("xyz"))
            .or()
            .when(containsString("test"));

        // Then: Error is added because second condition matches
        assertThat(validationResult.hasErrors(), is(true));
    }
}
