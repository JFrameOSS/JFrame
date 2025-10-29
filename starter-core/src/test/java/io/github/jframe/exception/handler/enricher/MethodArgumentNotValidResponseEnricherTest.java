package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResource;
import io.github.jframe.util.converter.ModelConverter;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.springframework.http.HttpStatus;
import org.springframework.validation.BindingResult;
import org.springframework.validation.FieldError;
import org.springframework.validation.ObjectError;
import org.springframework.web.bind.MethodArgumentNotValidException;
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
 * Tests for {@link MethodArgumentNotValidResponseEnricher}.
 *
 * <p>Verifies the MethodArgumentNotValidResponseEnricher functionality including:
 * <ul>
 * <li>Validation error enrichment for MethodArgumentNotValidException</li>
 * <li>Conditional enrichment (only for MethodArgumentNotValidException and MethodArgumentNotValidResponseResource)</li>
 * <li>ModelConverter integration for ObjectError to ValidationErrorResource conversion</li>
 * <li>Handling of empty validation error lists</li>
 * <li>No enrichment for non-MethodArgumentNotValidException throwables</li>
 * </ul>
 */
@SuppressWarnings("unchecked")
@DisplayName("Exception Response Enrichers - Method Argument Not Valid Response Enricher")
public class MethodArgumentNotValidResponseEnricherTest extends UnitTest {

    @Mock
    private ModelConverter<ObjectError, ValidationErrorResource> objectErrorResourceAssembler;

    @Test
    @DisplayName("Should enrich validation errors when throwable is MethodArgumentNotValidException")
    public void shouldEnrichValidationErrorsWhenThrowableIsMethodArgumentNotValidException() {
        // Given: A method argument not valid response resource, exception with binding errors, and mocked converter
        final MethodArgumentNotValidResponseResource resource = new MethodArgumentNotValidResponseResource();
        final BindingResult bindingResult = mock(BindingResult.class);
        final FieldError fieldError1 = new FieldError("user", "email", "must not be null");
        final FieldError fieldError2 = new FieldError("user", "name", "must not be blank");
        final List<ObjectError> errors = List.of(fieldError1, fieldError2);
        when(bindingResult.getAllErrors()).thenReturn(errors);

        final MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        final WebRequest request = mock(WebRequest.class);

        final ValidationErrorResource errorResource1 = new ValidationErrorResource();
        errorResource1.setField("email");
        errorResource1.setCode("REQUIRED");
        final ValidationErrorResource errorResource2 = new ValidationErrorResource();
        errorResource2.setField("name");
        errorResource2.setCode("REQUIRED");
        final List<ValidationErrorResource> convertedErrors = List.of(errorResource1, errorResource2);

        when(objectErrorResourceAssembler.convert(any(List.class))).thenReturn(convertedErrors);

        final MethodArgumentNotValidResponseEnricher enricher = new MethodArgumentNotValidResponseEnricher(objectErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Validation errors are set and converter is called
        assertThat(resource.getErrors(), hasSize(2));
        verify(objectErrorResourceAssembler).convert(any(List.class));
    }

    @Test
    @DisplayName("Should not enrich when validation errors list is empty")
    public void shouldNotEnrichWhenValidationErrorsListIsEmpty() {
        // Given: A method argument not valid response resource and exception with no errors
        final MethodArgumentNotValidResponseResource resource = new MethodArgumentNotValidResponseResource();
        final BindingResult bindingResult = mock(BindingResult.class);
        when(bindingResult.getAllErrors()).thenReturn(List.of());

        final MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        final WebRequest request = mock(WebRequest.class);

        final MethodArgumentNotValidResponseEnricher enricher = new MethodArgumentNotValidResponseEnricher(objectErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Errors remain null and converter is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(objectErrorResourceAssembler, never()).convert(anyIterable());
    }

    @Test
    @DisplayName("Should not enrich when throwable is not MethodArgumentNotValidException")
    public void shouldNotEnrichWhenThrowableIsNotMethodArgumentNotValidException() {
        // Given: A method argument not valid response resource and a regular exception
        final MethodArgumentNotValidResponseResource resource = new MethodArgumentNotValidResponseResource();
        final RuntimeException exception = new RuntimeException("Regular error");
        final WebRequest request = mock(WebRequest.class);

        final MethodArgumentNotValidResponseEnricher enricher = new MethodArgumentNotValidResponseEnricher(objectErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Errors remain null and converter is not called
        assertThat(resource.getErrors(), is(nullValue()));
        verify(objectErrorResourceAssembler, never()).convert(anyIterable());
    }

    @Test
    @DisplayName("Should not enrich when resource is not MethodArgumentNotValidResponseResource")
    public void shouldNotEnrichWhenResourceIsNotMethodArgumentNotValidResponseResource() {
        // Given: A regular error response resource and a MethodArgumentNotValidException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final BindingResult bindingResult = mock(BindingResult.class);

        final MethodArgumentNotValidException validationException = new MethodArgumentNotValidException(null, bindingResult);
        final WebRequest request = mock(WebRequest.class);

        final MethodArgumentNotValidResponseEnricher enricher = new MethodArgumentNotValidResponseEnricher(objectErrorResourceAssembler);

        // When: Enriching the response
        enricher.doEnrich(resource, validationException, request, HttpStatus.BAD_REQUEST);

        // Then: Converter is not called and binding result is never accessed
        verify(objectErrorResourceAssembler, never()).convert(anyIterable());
    }
}
