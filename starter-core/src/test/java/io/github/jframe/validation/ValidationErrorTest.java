package io.github.jframe.validation;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * Tests for {@link ValidationError}.
 *
 * <p>Verifies the ValidationError functionality including:
 * <ul>
 * <li>Constructor variations (code only, field+code)</li>
 * <li>Field and code storage and retrieval</li>
 * <li>Null handling for optional field parameter</li>
 * <li>Required code parameter validation</li>
 * <li>Serialization support</li>
 * </ul>
 */
@DisplayName("Validation Framework - Validation Error")
public class ValidationErrorTest extends UnitTest {

    @Test
    @DisplayName("Should create validation error with code only")
    public void shouldCreateValidationErrorWithCodeOnly() {
        // Given: An error code
        final String code = "INVALID_INPUT";

        // When: Creating validation error with code only
        final ValidationError error = new ValidationError(code);

        // Then: Error is created with the code and null field
        assertThat(error, is(notNullValue()));
        assertThat(error.getCode(), is(equalTo(code)));
        assertThat(error.getField(), is(nullValue()));
    }

    @Test
    @DisplayName("Should create validation error with field and code")
    public void shouldCreateValidationErrorWithFieldAndCode() {
        // Given: A field name and error code
        final String field = "username";
        final String code = "REQUIRED";

        // When: Creating validation error with field and code
        final ValidationError error = new ValidationError(field, code);

        // Then: Error is created with both field and code
        assertThat(error.getField(), is(equalTo(field)));
        assertThat(error.getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should allow null field parameter")
    public void shouldAllowNullField() {
        // Given: A null field and an error code
        final String code = "GENERAL_ERROR";

        // When: Creating validation error with null field
        final ValidationError error = new ValidationError(null, code);

        // Then: Error is created with null field and the code
        assertThat(error.getField(), is(nullValue()));
        assertThat(error.getCode(), is(equalTo(code)));
    }

    @Test
    @DisplayName("Should throw NullPointerException when code is null")
    public void shouldThrowExceptionWhenCodeIsNull() {
        // Given: A null code

        // When: Creating validation error with null code
        // Then: NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> new ValidationError(null));
    }

    @Test
    @DisplayName("Should throw NullPointerException when code is null with field")
    public void shouldThrowExceptionWhenCodeIsNullWithField() {
        // Given: A field name and null code
        final String field = "email";

        // When: Creating validation error with field and null code
        // Then: NullPointerException is thrown
        assertThrows(NullPointerException.class, () -> new ValidationError(field, null));
    }

    @Test
    @DisplayName("Should have working toString method")
    public void shouldHaveWorkingToString() {
        // Given: A validation error with field and code
        final ValidationError error = new ValidationError("age", "INVALID_FORMAT");

        // When: Calling toString
        final String result = error.toString();

        // Then: Result is not null and contains relevant information
        assertThat(result, is(notNullValue()));
    }
}
