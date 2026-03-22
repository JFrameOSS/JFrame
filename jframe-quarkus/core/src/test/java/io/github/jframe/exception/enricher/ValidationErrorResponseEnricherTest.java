package io.github.jframe.exception.enricher;

import io.github.jframe.exception.assembler.ValidationErrorResourceAssembler;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;

import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyList;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ValidationErrorResponseEnricher}.
 *
 * <p>Verifies the enricher correctly populates validation errors using the assembler including:
 * <ul>
 * <li>Converts ValidationError list to ValidationErrorResource list via assembler</li>
 * <li>Sets errors on ValidationErrorResponseResource</li>
 * <li>No enrichment when throwable is not ValidationException</li>
 * <li>No enrichment when resource is not ValidationErrorResponseResource</li>
 * <li>No enrichment when errors list is empty</li>
 * </ul>
 */
@DisplayName("Unit Test - Validation Error Response Enricher")
public class ValidationErrorResponseEnricherTest extends UnitTest {

    @Mock
    private ValidationErrorResourceAssembler assembler;

    @Test
    @DisplayName("Should enrich validation errors when throwable is ValidationException and resource is correct type")
    public void shouldEnrichValidationErrorsWhenThrowableIsValidationExceptionAndResourceIsCorrectType() {
        // Given: A ValidationErrorResponseResource, ValidationException with errors, and a mocked assembler
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.rejectValue("email", "REQUIRED");
        validationResult.rejectValue("name", "INVALID");
        final ValidationException validationException = new ValidationException(validationResult);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResource errorResource1 = new ValidationErrorResource();
        errorResource1.setField("email");
        errorResource1.setCode("REQUIRED");
        final ValidationErrorResource errorResource2 = new ValidationErrorResource();
        errorResource2.setField("name");
        errorResource2.setCode("INVALID");
        final List<ValidationErrorResource> convertedErrors = List.of(errorResource1, errorResource2);

        when(assembler.convert(anyList())).thenReturn(convertedErrors);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, requestContext, 400);

        // Then: Validation errors are set and assembler was invoked
        assertThat(resource.getErrors(), hasSize(2));
        verify(assembler).convert(anyList());
    }

    @Test
    @DisplayName("Should not enrich when validation errors list is empty")
    public void shouldNotEnrichWhenValidationErrorsListIsEmpty() {
        // Given: A ValidationErrorResponseResource and ValidationException with no errors
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        final ValidationException validationException = new ValidationException(validationResult);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, requestContext, 400);

        // Then: Errors remain null and assembler is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(assembler, never()).convert(anyList());
    }

    @Test
    @DisplayName("Should not enrich when throwable is not ValidationException")
    public void shouldNotEnrichWhenThrowableIsNotValidationException() {
        // Given: A ValidationErrorResponseResource but a RuntimeException
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Generic error");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Errors remain null and assembler is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(assembler, never()).convert(anyList());
    }

    @Test
    @DisplayName("Should not enrich when resource is not ValidationErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotValidationErrorResponseResource() {
        // Given: A base ErrorResponseResource but a ValidationException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.rejectValue("email", "REQUIRED");
        final ValidationException validationException = new ValidationException(validationResult);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(assembler);

        // When: Enriching the response (type mismatch)
        enricher.doEnrich(resource, validationException, requestContext, 400);

        // Then: Assembler is not called
        verify(assembler, never()).convert(anyList());
    }

    @Test
    @DisplayName("Should convert single validation error via assembler")
    public void shouldConvertSingleValidationErrorViaAssembler() {
        // Given: A ValidationErrorResponseResource and a ValidationException with one error
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationError validationError = new ValidationError("id", "NOT_FOUND");
        final ValidationException validationException = new ValidationException(validationError);
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        final ValidationErrorResource singleResource = new ValidationErrorResource();
        singleResource.setField("id");
        singleResource.setCode("NOT_FOUND");

        when(assembler.convert(anyList())).thenReturn(List.of(singleResource));

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(assembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, requestContext, 400);

        // Then: Single error is set
        assertThat(resource.getErrors(), hasSize(1));
    }
}
