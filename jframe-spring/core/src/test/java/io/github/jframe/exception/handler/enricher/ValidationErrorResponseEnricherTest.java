package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.util.converter.ModelConverter;
import io.github.jframe.validation.ValidationError;
import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasSize;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyIterable;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ValidationErrorResponseEnricher}.
 *
 * <p>Verifies the ValidationErrorResponseEnricher functionality including:
 * <ul>
 * <li>Validation error enrichment for ValidationException</li>
 * <li>Conditional enrichment (only for ValidationException and ValidationErrorResponseResource)</li>
 * <li>ModelConverter integration for error resource conversion</li>
 * <li>Handling of empty validation error lists</li>
 * <li>No enrichment for non-ValidationException throwables</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
@DisplayName("Exception Response Enrichers - Validation Error Response Enricher")
public class ValidationErrorResponseEnricherTest extends UnitTest {

    @Mock
    private ModelConverter<ValidationError, ValidationErrorResource> validationErrorResourceAssembler;

    @Test
    @DisplayName("Should enrich validation errors when throwable is ValidationException")
    public void shouldEnrichValidationErrorsWhenThrowableIsValidationException() {
        // Given: A validation error response resource, ValidationException with errors, and mocked converter
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.rejectValue("email", "REQUIRED");
        validationResult.rejectValue("name", "INVALID");
        final ValidationException validationException = new ValidationException(validationResult);
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResource errorResource1 = new ValidationErrorResource();
        errorResource1.setField("email");
        errorResource1.setCode("REQUIRED");
        final ValidationErrorResource errorResource2 = new ValidationErrorResource();
        errorResource2.setField("name");
        errorResource2.setCode("INVALID");
        final List<ValidationErrorResource> convertedErrors = List.of(errorResource1, errorResource2);

        when(validationErrorResourceAssembler.convert(any(List.class))).thenReturn(convertedErrors);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(validationErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Validation errors are set and converter is called
        assertThat(resource.getErrors(), hasSize(2));
        verify(validationErrorResourceAssembler).convert(any(List.class));
    }

    @Test
    @DisplayName("Should not enrich when validation errors list is empty")
    public void shouldNotEnrichWhenValidationErrorsListIsEmpty() {
        // Given: A validation error response resource and ValidationException with no errors
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        final ValidationException validationException = new ValidationException(validationResult);
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(validationErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Errors remain null and converter is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(validationErrorResourceAssembler, never()).convert(anyIterable());
    }

    @Test
    @DisplayName("Should not enrich when throwable is not ValidationException")
    public void shouldNotEnrichWhenThrowableIsNotValidationException() {
        // Given: A validation error response resource and a regular exception
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(validationErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Errors remain null and converter is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(validationErrorResourceAssembler, never()).convert(anyIterable());
    }

    @Test
    @DisplayName("Should not enrich when resource is not ValidationErrorResponseResource")
    public void shouldNotEnrichWhenResourceIsNotValidationErrorResponseResource() {
        // Given: A regular error response resource and a ValidationException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        validationResult.rejectValue("email", "REQUIRED");
        final ValidationException validationException = new ValidationException(validationResult);
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(validationErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Converter is not called
        verify(validationErrorResourceAssembler, never()).convert(anyIterable());
    }

    @Test
    @DisplayName("Should not enrich when validation errors list is null")
    public void shouldNotEnrichWhenValidationErrorsListIsNull() {
        // Given: A validation error response resource and ValidationException with null errors (shouldn't happen in practice)
        final ValidationErrorResponseResource resource = new ValidationErrorResponseResource();
        final ValidationResult validationResult = new ValidationResult();
        final ValidationException validationException = new ValidationException(validationResult);
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResponseEnricher enricher = new ValidationErrorResponseEnricher(validationErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Errors remain null
        assertThat(resource.getErrors(), is(nullValue()));
    }
}
