package io.github.jframe.exception.enricher;

import io.github.jframe.exception.JFrameException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;
import io.github.support.fixtures.TestApiError;
import io.github.support.fixtures.TestApiException;

import jakarta.ws.rs.container.ContainerRequestContext;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorMessageResponseEnricher}.
 *
 * <p>Verifies the enricher correctly sets error message on the response resource including:
 * <ul>
 * <li>Message from JFrameException is set directly</li>
 * <li>Non-JFrameException (uncaught) uses generic "Internal server error" to avoid stack trace leaking</li>
 * <li>Null message from JFrameException is handled</li>
 * </ul>
 */
@DisplayName("Unit Test - Error Message Response Enricher")
public class ErrorMessageResponseEnricherTest extends UnitTest {

    private ErrorMessageResponseEnricher enricher;

    @BeforeEach
    public void setUp() {
        enricher = new ErrorMessageResponseEnricher();
    }

    @Test
    @DisplayName("Should set error message from JFrameException message")
    public void shouldSetErrorMessageFromJFrameExceptionMessage() {
        // Given: An error response resource and a JFrameException with a message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final JFrameException exception = new BadRequestException("Invalid request parameter");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: Error message is set from the exception
        assertThat(resource.getErrorMessage(), is(equalTo("Invalid request parameter")));
    }

    @Test
    @DisplayName("Should set error message from ApiException message")
    public void shouldSetErrorMessageFromApiExceptionMessage() {
        // Given: An error response resource and an ApiException with a message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final TestApiException exception = new TestApiException(new TestApiError("CODE", "reason"), "Api error occurred");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 400);

        // Then: Error message is set from the ApiException
        assertThat(resource.getErrorMessage(), is(equalTo("Api error occurred")));
    }

    @Test
    @DisplayName("Should use generic message for non-JFrameException to avoid stack trace leaking")
    public void shouldUseGenericMessageForNonJFrameExceptionToAvoidStackTraceLeaking() {
        // Given: An error response resource and a generic RuntimeException (not a JFrameException)
        final ErrorResponseResource resource = new ErrorResponseResource();
        final RuntimeException exception = new RuntimeException("sensitive internal details: DB connection failed at server:1234");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Generic message is used (not the sensitive internal message)
        assertThat(resource.getErrorMessage(), is(equalTo("Internal server error")));
    }

    @Test
    @DisplayName("Should use generic message for NullPointerException (uncaught exception)")
    public void shouldUseGenericMessageForNullPointerException() {
        // Given: An error response resource and a NullPointerException
        final ErrorResponseResource resource = new ErrorResponseResource();
        final NullPointerException exception = new NullPointerException("null at io.github.SomeClass.method:42");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Generic message is used
        assertThat(resource.getErrorMessage(), is(equalTo("Internal server error")));
    }

    @Test
    @DisplayName("Should handle JFrameException with null message")
    public void shouldHandleJFrameExceptionWithNullMessage() {
        // Given: An error response resource and a JFrameException with no message
        final ErrorResponseResource resource = new ErrorResponseResource();
        final JFrameException exception = new JFrameException();
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, exception, requestContext, 500);

        // Then: Error message is null (no message to set)
        assertThat(resource.getErrorMessage(), is(nullValue()));
    }

    @Test
    @DisplayName("Should use generic message for Error subclass (not JFrameException)")
    public void shouldUseGenericMessageForErrorSubclass() {
        // Given: An error response resource and an OutOfMemoryError
        final ErrorResponseResource resource = new ErrorResponseResource();
        final OutOfMemoryError error = new OutOfMemoryError("Java heap space at ...");
        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

        // When: Enriching the response
        enricher.doEnrich(resource, error, requestContext, 500);

        // Then: Generic message is used
        assertThat(resource.getErrorMessage(), is(equalTo("Internal server error")));
    }
}
