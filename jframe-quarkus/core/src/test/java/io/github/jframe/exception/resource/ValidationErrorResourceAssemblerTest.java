package io.github.jframe.exception.resource;

import io.github.jframe.exception.assembler.ValidationErrorResourceAssembler;
import io.github.jframe.validation.ValidationError;
import io.github.support.UnitTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link ValidationErrorResourceAssembler}.
 *
 * <p>Verifies the assembler correctly converts ValidationError list to ValidationErrorResource list including:
 * <ul>
 * <li>Field and code mapped from ValidationError to ValidationErrorResource</li>
 * <li>PropertyNamingStrategy applied to field and code names</li>
 * <li>No naming strategy: names passed through unchanged</li>
 * <li>Empty list produces empty result</li>
 * </ul>
 */
@DisplayName("Unit Test - Validation Error Resource Assembler")
public class ValidationErrorResourceAssemblerTest extends UnitTest {

    @Test
    @DisplayName("Should convert ValidationError list to ValidationErrorResource list")
    public void shouldConvertValidationErrorListToValidationErrorResourceList() {
        // Given: An assembler with no naming strategy and a list of validation errors
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ValidationErrorResourceAssembler assembler = new ValidationErrorResourceAssembler(objectMapper);

        final List<ValidationError> errors = List.of(
            new ValidationError("email", "REQUIRED"),
            new ValidationError("name", "INVALID")
        );

        // When: Converting the error list
        final List<ValidationErrorResource> result = assembler.convert(errors);

        // Then: All errors are converted with correct field and code
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getField(), is(equalTo("email")));
        assertThat(result.get(0).getCode(), is(equalTo("REQUIRED")));
        assertThat(result.get(1).getField(), is(equalTo("name")));
        assertThat(result.get(1).getCode(), is(equalTo("INVALID")));
    }

    @Test
    @DisplayName("Should apply snake_case naming strategy to field and code names")
    public void shouldApplySnakeCaseNamingStrategyToFieldAndCodeNames() {
        // Given: An assembler with SNAKE_CASE naming strategy
        final ObjectMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
        final ValidationErrorResourceAssembler assembler = new ValidationErrorResourceAssembler(objectMapper);

        final List<ValidationError> errors = List.of(
            new ValidationError("discountPrice", "InvalidLength"),
            new ValidationError("firstName", "NotBlank")
        );

        // When: Converting the error list
        final List<ValidationErrorResource> result = assembler.convert(errors);

        // Then: Field and code names are converted to snake_case
        assertThat(result, hasSize(2));
        assertThat(result.get(0).getField(), is(equalTo("discount_price")));
        assertThat(result.get(0).getCode(), is(equalTo("invalid_length")));
        assertThat(result.get(1).getField(), is(equalTo("first_name")));
        assertThat(result.get(1).getCode(), is(equalTo("not_blank")));
    }

    @Test
    @DisplayName("Should return empty list when input is empty")
    public void shouldReturnEmptyListWhenInputIsEmpty() {
        // Given: An assembler and an empty error list
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ValidationErrorResourceAssembler assembler = new ValidationErrorResourceAssembler(objectMapper);

        // When: Converting an empty list
        final List<ValidationErrorResource> result = assembler.convert(List.of());

        // Then: Result is empty
        assertThat(result, is(notNullValue()));
        assertThat(result, is(empty()));
    }

    @Test
    @DisplayName("Should pass through unchanged when no naming strategy configured")
    public void shouldPassThroughUnchangedWhenNoNamingStrategyConfigured() {
        // Given: An assembler with no naming strategy
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ValidationErrorResourceAssembler assembler = new ValidationErrorResourceAssembler(objectMapper);

        final List<ValidationError> errors = List.of(
            new ValidationError("simpleField", "SIMPLE_CODE")
        );

        // When: Converting the error list
        final List<ValidationErrorResource> result = assembler.convert(errors);

        // Then: Field and code are unchanged
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getField(), is(equalTo("simpleField")));
        assertThat(result.get(0).getCode(), is(equalTo("SIMPLE_CODE")));
    }

    @Test
    @DisplayName("Should handle ValidationError with null field (object-level error)")
    public void shouldHandleValidationErrorWithNullFieldObjectLevelError() {
        // Given: An assembler and a ValidationError with null field
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ValidationErrorResourceAssembler assembler = new ValidationErrorResourceAssembler(objectMapper);

        final List<ValidationError> errors = List.of(
            new ValidationError("OBJECT_LEVEL_ERROR")
        );

        // When: Converting the error list
        final List<ValidationErrorResource> result = assembler.convert(errors);

        // Then: Null field is preserved and code is set
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getField(), is(equalTo(null)));
        assertThat(result.get(0).getCode(), is(equalTo("OBJECT_LEVEL_ERROR")));
    }
}
