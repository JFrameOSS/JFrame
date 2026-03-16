package io.github.jframe.exception.factory;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.validation.ValidationResult;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;
import io.github.support.fixtures.TestApiException;

import java.time.OffsetDateTime;
import jakarta.validation.ConstraintViolationException;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link DefaultErrorResponseFactory}.
 *
 * <p>Verifies that the factory correctly maps exception types to response resource types including:
 * <ul>
 * <li>ApiException → ApiErrorResponseResource</li>
 * <li>ValidationException → ValidationErrorResponseResource</li>
 * <li>RateLimitExceededException → RateLimitErrorResponseResource</li>
 * <li>ConstraintViolationException → ConstraintViolationResponseResource</li>
 * <li>Cause chain traversal for JFrameException wrapped in RuntimeException</li>
 * <li>Unknown exception → base ErrorResponseResource</li>
 * </ul>
 */
@DisplayName("Unit Test - Default Error Response Factory")
public class DefaultErrorResponseFactoryTest extends UnitTest {

    private DefaultErrorResponseFactory factory;

    @BeforeEach
    public void setUp() {
        factory = new DefaultErrorResponseFactory();
    }

    @Test
    @DisplayName("Should create ApiErrorResponseResource for ApiException")
    public void shouldCreateApiErrorResponseResourceForApiException() {
        // Given: An ApiException
        final ApiException exception = new TestApiException(new TestApiError("ERR001", "Test error"));

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is ApiErrorResponseResource
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(ApiErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should create ValidationErrorResponseResource for ValidationException")
    public void shouldCreateValidationErrorResponseResourceForValidationException() {
        // Given: A ValidationException
        final ValidationResult validationResult = new ValidationResult();
        validationResult.rejectValue("field", "REQUIRED");
        final ValidationException exception = new ValidationException(validationResult);

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is ValidationErrorResponseResource
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(ValidationErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should create RateLimitErrorResponseResource for RateLimitExceededException")
    public void shouldCreateRateLimitErrorResponseResourceForRateLimitExceededException() {
        // Given: A RateLimitExceededException
        final RateLimitExceededException exception = new RateLimitExceededException(100, 0, OffsetDateTime.now().plusMinutes(5));

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is RateLimitErrorResponseResource
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(RateLimitErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should create ConstraintViolationResponseResource for ConstraintViolationException")
    public void shouldCreateConstraintViolationResponseResourceForConstraintViolationException() {
        // Given: A ConstraintViolationException
        final ConstraintViolationException exception = new ConstraintViolationException(
            "Validation failed",
            java.util.Collections.emptySet()
        );

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is ConstraintViolationResponseResource
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(ConstraintViolationResponseResource.class)));
    }

    @Test
    @DisplayName("Should create base ErrorResponseResource for unknown exception type")
    public void shouldCreateBaseErrorResponseResourceForUnknownExceptionType() {
        // Given: A generic RuntimeException (not a JFrame exception)
        final RuntimeException exception = new RuntimeException("Unexpected error");

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is base ErrorResponseResource (not a subclass)
        assertThat(resource, is(notNullValue()));
        assertThat(resource.getClass(), is(ErrorResponseResource.class));
    }

    @Test
    @DisplayName("Should traverse cause chain and find ApiException wrapped in RuntimeException")
    public void shouldTraverseCauseChainAndFindApiExceptionWrappedInRuntimeException() {
        // Given: An ApiException wrapped inside a RuntimeException
        final ApiException apiException = new TestApiException(new TestApiError("WRAPPED", "Wrapped error"), "api error message");
        final RuntimeException wrappedException = new RuntimeException("Outer exception", apiException);

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(wrappedException);

        // Then: Resource is ApiErrorResponseResource (cause chain was traversed)
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(ApiErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should traverse cause chain and find ValidationException wrapped in RuntimeException")
    public void shouldTraverseCauseChainAndFindValidationExceptionWrappedInRuntimeException() {
        // Given: A ValidationException wrapped inside a RuntimeException
        final ValidationException validationException = new ValidationException(new ValidationResult());
        final RuntimeException wrappedException = new RuntimeException("Outer exception", validationException);

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(wrappedException);

        // Then: Resource is ValidationErrorResponseResource (cause chain was traversed)
        assertThat(resource, is(notNullValue()));
        assertThat(resource, is(instanceOf(ValidationErrorResponseResource.class)));
    }

    @Test
    @DisplayName("Should create base ErrorResponseResource when cause chain contains only unknown exceptions")
    public void shouldCreateBaseErrorResponseResourceWhenCauseChainContainsOnlyUnknownExceptions() {
        // Given: A RuntimeException with another RuntimeException as cause (no JFrameException)
        final RuntimeException cause = new RuntimeException("Root cause");
        final RuntimeException exception = new RuntimeException("Outer exception", cause);

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource is base ErrorResponseResource
        assertThat(resource, is(notNullValue()));
        assertThat(resource.getClass(), is(ErrorResponseResource.class));
    }

    @Test
    @DisplayName("Should store throwable reference in created resource")
    public void shouldStoreThrowableReferenceInCreatedResource() {
        // Given: A generic RuntimeException
        final RuntimeException exception = new RuntimeException("Some error");

        // When: Creating error response resource
        final ErrorResponseResource resource = factory.create(exception);

        // Then: Resource holds reference to throwable
        assertThat(resource.getThrowable(), is(notNullValue()));
    }
}
