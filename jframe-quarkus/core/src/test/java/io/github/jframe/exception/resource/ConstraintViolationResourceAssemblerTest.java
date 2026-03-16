package io.github.jframe.exception.resource;

import io.github.support.UnitTest;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.PropertyNamingStrategies;
import tools.jackson.databind.json.JsonMapper;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Path;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ConstraintViolationResourceAssembler}.
 *
 * <p>Verifies the assembler correctly converts ConstraintViolation set to ValidationErrorResource list including:
 * <ul>
 * <li>Field from property path, code from constraint annotation simple name</li>
 * <li>PropertyNamingStrategy applied to field names</li>
 * <li>Empty set produces empty result</li>
 * </ul>
 */
@DisplayName("Unit Test - Constraint Violation Resource Assembler")
public class ConstraintViolationResourceAssemblerTest extends UnitTest {

    @Test
    @DisplayName("Should convert ConstraintViolation set to ValidationErrorResource list")
    public void shouldConvertConstraintViolationSetToValidationErrorResourceList() {
        // Given: An assembler with no naming strategy and a set of constraint violations
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ConstraintViolationResourceAssembler assembler = new ConstraintViolationResourceAssembler(objectMapper);

        @SuppressWarnings("unchecked") final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("email");
        when(violation.getPropertyPath()).thenReturn(path);

        // Simulate annotation type (NotNull → "NotNull" as code)
        @SuppressWarnings(
            "unchecked"
        ) final jakarta.validation.metadata.ConstraintDescriptor<jakarta.validation.constraints.NotNull> descriptor = mock(
            jakarta.validation.metadata.ConstraintDescriptor.class
        );
        stubConstraintDescriptor(violation, descriptor);
        when(descriptor.getAnnotation()).thenReturn(mock(jakarta.validation.constraints.NotNull.class));

        final Set<ConstraintViolation<?>> violations = Set.of(violation);

        // When: Converting the violation set
        final List<ValidationErrorResource> result = assembler.convert(violations);

        // Then: Result is not null and contains one entry
        assertThat(result, is(notNullValue()));
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getField(), is(equalTo("email")));
        assertThat(result.get(0).getCode(), is(equalTo("NotNull")));
    }

    @Test
    @DisplayName("Should apply snake_case naming strategy to field from property path")
    public void shouldApplySnakeCaseNamingStrategyToFieldFromPropertyPath() {
        // Given: An assembler with SNAKE_CASE naming strategy and a violation with camelCase path
        final ObjectMapper objectMapper = JsonMapper.builder()
            .propertyNamingStrategy(PropertyNamingStrategies.SNAKE_CASE)
            .build();
        final ConstraintViolationResourceAssembler assembler = new ConstraintViolationResourceAssembler(objectMapper);

        @SuppressWarnings("unchecked") final ConstraintViolation<Object> violation = mock(ConstraintViolation.class);
        final Path path = mock(Path.class);
        when(path.toString()).thenReturn("discountPrice");
        when(violation.getPropertyPath()).thenReturn(path);

        @SuppressWarnings(
            "unchecked"
        ) final jakarta.validation.metadata.ConstraintDescriptor<jakarta.validation.constraints.NotBlank> descriptor = mock(
            jakarta.validation.metadata.ConstraintDescriptor.class
        );
        stubConstraintDescriptor(violation, descriptor);
        when(descriptor.getAnnotation()).thenReturn(mock(jakarta.validation.constraints.NotBlank.class));

        final Set<ConstraintViolation<?>> violations = Set.of(violation);

        // When: Converting the violation set
        final List<ValidationErrorResource> result = assembler.convert(violations);

        // Then: Field is converted to snake_case
        assertThat(result, hasSize(1));
        assertThat(result.get(0).getField(), is(equalTo("discount_price")));
        assertThat(result.get(0).getCode(), is(equalTo("NotBlank")));
    }

    @SuppressWarnings("unchecked")
    private static void stubConstraintDescriptor(
        final ConstraintViolation<Object> violation,
        final jakarta.validation.metadata.ConstraintDescriptor<?> descriptor
    ) {
        when(violation.getConstraintDescriptor())
            .thenReturn((jakarta.validation.metadata.ConstraintDescriptor) descriptor);
    }

    @Test
    @DisplayName("Should return empty list when violations set is empty")
    public void shouldReturnEmptyListWhenViolationsSetIsEmpty() {
        // Given: An assembler and an empty violations set
        final ObjectMapper objectMapper = JsonMapper.builder().build();
        final ConstraintViolationResourceAssembler assembler = new ConstraintViolationResourceAssembler(objectMapper);

        // When: Converting empty set
        final List<ValidationErrorResource> result = assembler.convert(Collections.emptySet());

        // Then: Result is empty
        assertThat(result, is(notNullValue()));
        assertThat(result, is(empty()));
    }
}
