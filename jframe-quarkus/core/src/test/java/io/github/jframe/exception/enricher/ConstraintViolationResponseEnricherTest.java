package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ConstraintViolationResourceAssembler;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.support.UnitTest;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.ConstraintViolationException;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anySet;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ConstraintViolationResponseEnricher}.
 *
 * <p>Verifies the enricher correctly populates constraint violations using the assembler including:
 * <ul>
 * <li>Converts ConstraintViolation set to ValidationErrorResource list via assembler</li>
 * <li>Sets errors on ConstraintViolationResponseResource</li>
 * <li>No enrichment when throwable is not ConstraintViolationException</li>
 * <li>No enrichment when resource is not ConstraintViolationResponseResource</li>
 * </ul>
 */
@DisplayName("Unit Test - Constraint Violation Response Enricher")
public class ConstraintViolationResponseEnricherTest extends UnitTest {

    @Mock
    private ConstraintViolationResourceAssembler assembler;

    @Test
    @DisplayName("Should enrich constraint violations when throwable is ConstraintViolationException and resource is correct type")
    public void shouldEnrichConstraintViolationsWhenThrowableIsCorrectTypeAndResourceIsCorrectType() {
        // Given: A ConstraintViolationResponseResource and exception with violations
        final ConstraintViolationResponseResource resource = new ConstraintViolationResponseResource();
        @SuppressWarnings("unchecked") final ConstraintViolation<Object> violation1 = mock(ConstraintViolation.class);
        @SuppressWarnings("unchecked") final ConstraintViolation<Object> violation2 = mock(ConstraintViolation.class);
        final Set<ConstraintViolation<?>> violations = Set.of(violation1, violation2);
        final ConstraintViolationException exception = new ConstraintViolationException("Validation failed", violations);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResource errorResource1 = new ValidationErrorResource();
        errorResource1.setField("email");
        errorResource1.setCode("NotNull");
        final ValidationErrorResource errorResource2 = new ValidationErrorResource();
        errorResource2.setField("name");
        errorResource2.setCode("NotBlank");

        when(assembler.convert(anySet())).thenReturn(List.of(errorResource1, errorResource2));

        final ConstraintViolationResponseEnricher enricher = new ConstraintViolationResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: Errors are set via assembler
        assertThat(resource.getErrors(), hasSize(2));
        verify(assembler).convert(anySet());
    }

    @Test
    @DisplayName("Should not enrich when throwable is not ConstraintViolationException")
    public void shouldNotEnrichWhenThrowableIsNotConstraintViolationException() {
        // Given: A ConstraintViolationResponseResource but a generic RuntimeException
        final ConstraintViolationResponseResource resource = new ConstraintViolationResponseResource();
        final RuntimeException exception = new RuntimeException("Generic error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ConstraintViolationResponseEnricher enricher = new ConstraintViolationResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Errors remain null and assembler is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(assembler, never()).convert(anySet());
    }

    @Test
    @DisplayName("Should not enrich when resource is not ConstraintViolationResponseResource")
    public void shouldNotEnrichWhenResourceIsNotConstraintViolationResponseResource() {
        // Given: A base ErrorResponseResource but a ConstraintViolationException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final ConstraintViolationException exception = new ConstraintViolationException("Violation", Collections.emptySet());
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ConstraintViolationResponseEnricher enricher = new ConstraintViolationResponseEnricher(assembler);

        // When: Enriching the response (type mismatch)
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: Assembler is not called
        verify(assembler, never()).convert(anySet());
    }

    @Test
    @DisplayName("Should not enrich when violations set is empty")
    public void shouldNotEnrichWhenViolationsSetIsEmpty() {
        // Given: A ConstraintViolationResponseResource and exception with no violations
        final ConstraintViolationResponseResource resource = new ConstraintViolationResponseResource();
        final ConstraintViolationException exception = new ConstraintViolationException("No violations", Collections.emptySet());
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ConstraintViolationResponseEnricher enricher = new ConstraintViolationResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: Errors remain null and assembler is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(assembler, never()).convert(anySet());
    }
}
