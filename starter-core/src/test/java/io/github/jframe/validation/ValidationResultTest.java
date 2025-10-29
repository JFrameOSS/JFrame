package io.github.jframe.validation;

import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.contains;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ValidationResult}.
 *
 * <p>Verifies the ValidationResult functionality including:
 * <ul>
 * <li>Error accumulation and retrieval</li>
 * <li>Nested path management (push/pop)</li>
 * <li>Rejection methods (reject, rejectValue, rejectIf, rejectValueIf)</li>
 * <li>Hamcrest matcher integration</li>
 * <li>Field rejection fluent API</li>
 * <li>Error merging from other ValidationResults</li>
 * <li>Indexed nested paths (e.g., items[0].name)</li>
 * </ul>
 */
@DisplayName("Validation Framework - Validation Result")
public class ValidationResultTest extends UnitTest {

    private ValidationResult validationResult;

    @Override
    @BeforeEach
    public void setUp() {
        validationResult = new ValidationResult();
    }

    @Test
    @DisplayName("Should start with no errors")
    public void shouldStartWithNoErrors() {
        // Given: A new validation result (set up in @BeforeEach)

        // When: Checking for errors
        final boolean hasErrors = validationResult.hasErrors();
        final List<ValidationError> errors = validationResult.getErrors();

        // Then: Result has no errors
        assertThat(hasErrors, is(false));
        assertThat(errors, is(empty()));
    }

    @Test
    @DisplayName("Should start with empty nested path")
    public void shouldStartWithEmptyNestedPath() {
        // Given: A new validation result (set up in @BeforeEach)

        // When: Getting the nested path
        final String nestedPath = validationResult.getNestedPath();

        // Then: Nested path is empty string
        assertThat(nestedPath, is(equalTo("")));
    }

    @Test
    @DisplayName("Should reject with global error code")
    public void shouldRejectWithGlobalErrorCode() {
        // Given: A validation result and an error code
        final String code = "VALIDATION_FAILED";

        // When: Rejecting with the error code
        validationResult.reject(code);

        // Then: Error is added without a field
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors(), hasSize(1));
        final ValidationError error = validationResult.getErrors().getFirst();
        assertThat(error.getField(), is(nullValue()));
        assertThat(error.getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should reject conditionally with rejectIf when condition is true")
    public void shouldRejectConditionallyWhenTrue() {
        // Given: A validation result and a true condition
        final boolean condition = true;
        final String code = "CONDITION_MET";

        // When: Rejecting if condition is true
        validationResult.rejectIf(condition, code);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should not reject with rejectIf when condition is false")
    public void shouldNotRejectWhenConditionIsFalse() {
        // Given: A validation result and a false condition
        final boolean condition = false;
        final String code = "CONDITION_MET";

        // When: Rejecting if condition is true
        validationResult.rejectIf(condition, code);

        // Then: No error is added
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should reject with matcher when matcher matches")
    public void shouldRejectWithMatcherWhenMatches() {
        // Given: A validation result, a value, and a matching matcher
        final String value = "test";
        final String code = "INVALID_VALUE";

        // When: Rejecting if matcher matches
        validationResult.rejectIf(value, containsString("es"), code);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors().getFirst().getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should not reject with matcher when matcher does not match")
    public void shouldNotRejectWithMatcherWhenDoesNotMatch() {
        // Given: A validation result, a value, and a non-matching matcher
        final String value = "test";
        final String code = "INVALID_VALUE";

        // When: Rejecting if matcher matches
        validationResult.rejectIf(value, containsString("xyz"), code);

        // Then: No error is added
        assertThat(validationResult.hasErrors(), is(false));
    }

    @Test
    @DisplayName("Should reject value with current nested path")
    public void shouldRejectValueWithCurrentNestedPath() {
        // Given: A validation result with a nested path
        final String code = "REQUIRED";
        validationResult.pushNestedPath("user");

        // When: Rejecting a value
        validationResult.rejectValue(code);

        // Then: Error is added with the nested path as field
        assertThat(validationResult.getErrors(), hasSize(1));
        final ValidationError error = validationResult.getErrors().getFirst();
        assertThat(error.getField(), is(equalTo("user")));
        assertThat(error.getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should reject value with field and nested path")
    public void shouldRejectValueWithFieldAndNestedPath() {
        // Given: A validation result with a nested path
        final String field = "email";
        final String code = "INVALID_FORMAT";
        validationResult.pushNestedPath("user");

        // When: Rejecting a field value
        validationResult.rejectValue(field, code);

        // Then: Error is added with combined nested path and field
        assertThat(validationResult.getErrors(), hasSize(1));
        final ValidationError error = validationResult.getErrors().getFirst();
        assertThat(error.getField(), is(equalTo("user.email")));
        assertThat(error.getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should reject value conditionally with rejectValueIf")
    public void shouldRejectValueConditionally() {
        // Given: A validation result and a true condition
        final boolean condition = true;
        final String code = "REQUIRED";

        // When: Rejecting value if condition is true
        validationResult.rejectValueIf(condition, code);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should reject value with matcher when matcher matches")
    public void shouldRejectValueWithMatcher() {
        // Given: A validation result, a value, and a matching matcher
        final Integer value = 5;
        final String code = "TOO_SMALL";

        // When: Rejecting value if matcher matches
        validationResult.rejectValueIf(value, is(equalTo(5)), code);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
    }

    @Test
    @DisplayName("Should reject value with field, matcher when matcher matches")
    public void shouldRejectValueWithFieldAndMatcher() {
        // Given: A validation result, field, value, and matching matcher
        final String field = "age";
        final Integer value = 150;
        final String code = "TOO_HIGH";

        // When: Rejecting value if matcher matches
        validationResult.rejectValueIf(value, is(equalTo(150)), field, code);

        // Then: Error is added with field
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getField(), is(equalTo(field)));
    }

    @Test
    @DisplayName("Should push and pop nested path")
    public void shouldPushAndPopNestedPath() {
        // Given: A validation result

        // When: Pushing and popping nested paths
        validationResult.pushNestedPath("user");
        final String firstPath = validationResult.getNestedPath();
        validationResult.pushNestedPath("address");
        final String secondPath = validationResult.getNestedPath();
        validationResult.popNestedPath();
        final String afterPop = validationResult.getNestedPath();

        // Then: Nested paths are managed correctly
        assertThat(firstPath, is(equalTo("user")));
        assertThat(secondPath, is(equalTo("user.address")));
        assertThat(afterPop, is(equalTo("user")));
    }

    @Test
    @DisplayName("Should push nested path with index")
    public void shouldPushNestedPathWithIndex() {
        // Given: A validation result with a nested path
        validationResult.pushNestedPath("items");

        // When: Pushing indexed nested path
        validationResult.pushNestedPath("item", 0);

        // Then: Nested path includes index
        assertThat(validationResult.getNestedPath(), is(equalTo("items.item[0]")));
    }

    @Test
    @DisplayName("Should throw exception when popping empty nested path stack")
    public void shouldThrowExceptionWhenPoppingEmptyStack() {
        // Given: A validation result with no nested paths

        // When: Attempting to pop nested path
        // Then: IllegalStateException is thrown
        assertThrows(IllegalStateException.class, () -> validationResult.popNestedPath());
    }

    @Test
    @DisplayName("Should add error directly")
    public void shouldAddErrorDirectly() {
        // Given: A validation error
        final ValidationError error = new ValidationError("field", "CODE");

        // When: Adding error directly
        validationResult.addError(error);

        // Then: Error is added
        assertThat(validationResult.hasErrors(), is(true));
        assertThat(validationResult.getErrors(), contains(error));
    }

    @Test
    @DisplayName("Should add all errors from list")
    public void shouldAddAllErrorsFromList() {
        // Given: A list of validation errors
        final List<ValidationError> errors = List.of(
            new ValidationError("field1", "CODE1"),
            new ValidationError("field2", "CODE2")
        );

        // When: Adding all errors
        validationResult.addAllErrors(errors);

        // Then: All errors are added
        assertThat(validationResult.getErrors(), hasSize(2));
    }

    @Test
    @DisplayName("Should merge errors from another ValidationResult")
    public void shouldMergeErrorsFromAnotherValidationResult() {
        // Given: Two validation results with errors
        final ValidationResult other = new ValidationResult();
        validationResult.reject("ERROR1");
        other.reject("ERROR2");
        other.reject("ERROR3");

        // When: Merging errors from other result
        validationResult.addAllErrors(other);

        // Then: All errors are combined
        assertThat(validationResult.getErrors(), hasSize(3));
    }

    @Test
    @DisplayName("Should return unmodifiable list of errors")
    public void shouldReturnUnmodifiableList() {
        // Given: A validation result with an error
        validationResult.reject("ERROR");

        // When: Getting errors and attempting to modify
        final List<ValidationError> errors = validationResult.getErrors();

        // Then: List modification throws exception
        assertThrows(UnsupportedOperationException.class, () -> errors.add(new ValidationError("TEST")));
    }

    @Test
    @DisplayName("Should create field rejection with rejectField")
    public void shouldCreateFieldRejection() {
        // Given: A validation result, field, and value
        final String field = "email";
        final String value = "invalid-email";

        // When: Creating field rejection
        final var fieldRejection = validationResult.rejectField(field, value);

        // Then: Field rejection is created
        assertThat(fieldRejection, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have working toString method")
    public void shouldHaveWorkingToString() {
        // Given: A validation result with errors
        validationResult.reject("ERROR1");
        validationResult.rejectValue("field", "ERROR2");

        // When: Calling toString
        final String result = validationResult.toString();

        // Then: Result is not null
        assertThat(result, is(notNullValue()));
    }

    @Test
    @DisplayName("Should handle complex nested paths with multiple levels")
    public void shouldHandleComplexNestedPaths() {
        // Given: A validation result

        // When: Building complex nested path
        validationResult.pushNestedPath("order");
        validationResult.pushNestedPath("items", 0);
        validationResult.pushNestedPath("product");
        validationResult.rejectValue("name", "REQUIRED");

        // Then: Error has complete nested path
        assertThat(validationResult.getErrors(), hasSize(1));
        assertThat(validationResult.getErrors().getFirst().getField(), is(equalTo("order.items[0].product.name")));
    }
}
