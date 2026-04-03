package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.mock;

/**
 * Tests for {@link ErrorResponseEnricher}.
 *
 * <p>Verifies the ErrorResponseEnricher interface functionality including:
 * <ul>
 * <li>Default enrich() method delegation to doEnrich()</li>
 * <li>Throwable extraction from ErrorResponseResource</li>
 * <li>Proper parameter passing to doEnrich()</li>
 * <li>Functional interface behavior</li>
 * </ul>
 */
@DisplayName("Exception Response Enrichers - Error Response Enricher Interface")
public class ErrorResponseEnricherTest extends UnitTest {

    @Test
    @DisplayName("Should delegate to doEnrich with throwable from error response resource")
    public void shouldDelegateToDoEnrichWithThrowableFromErrorResponseResource() {
        // Given: An error response resource with a throwable and a test enricher
        final RuntimeException throwable = new RuntimeException("Test error");
        final ErrorResponseResource resource = new ErrorResponseResource(throwable);
        final WebRequest request = mock(WebRequest.class);
        final HttpStatus httpStatus = HttpStatus.INTERNAL_SERVER_ERROR;

        final TestErrorResponseEnricher enricher = new TestErrorResponseEnricher();

        // When: Calling the default enrich method
        enricher.enrich(resource, request, httpStatus);

        // Then: doEnrich is called with the correct parameters
        assertThat(enricher.wasCalled, is(true));
        assertThat(enricher.capturedResource, is(equalTo(resource)));
        assertThat(enricher.capturedThrowable, is(equalTo(throwable)));
        assertThat(enricher.capturedRequest, is(equalTo(request)));
        assertThat(enricher.capturedHttpStatus, is(equalTo(httpStatus)));
    }

    @Test
    @DisplayName("Should handle null throwable in error response resource")
    public void shouldHandleNullThrowableInErrorResponseResource() {
        // Given: An error response resource with null throwable
        final ErrorResponseResource resource = new ErrorResponseResource(null);
        final WebRequest request = mock(WebRequest.class);
        final HttpStatus httpStatus = HttpStatus.BAD_REQUEST;

        final TestErrorResponseEnricher enricher = new TestErrorResponseEnricher();

        // When: Calling the default enrich method
        enricher.enrich(resource, request, httpStatus);

        // Then: doEnrich is called with null throwable
        assertThat(enricher.wasCalled, is(true));
        assertThat(enricher.capturedThrowable, is(equalTo(null)));
    }

    @Test
    @DisplayName("Should be functional interface")
    public void shouldBeFunctionalInterface() {
        // Given: A lambda implementation of ErrorResponseEnricher
        final ErrorResponseEnricher enricher = (resource, throwable, request, httpStatus) -> {
            resource.setErrorMessage("Enriched");
        };

        final ErrorResponseResource resource = new ErrorResponseResource();
        final WebRequest request = mock(WebRequest.class);

        // When: Using the lambda enricher
        enricher.doEnrich(resource, new RuntimeException(), request, HttpStatus.INTERNAL_SERVER_ERROR);

        // Then: Enricher is functional and works
        assertThat(resource.getErrorMessage(), is(equalTo("Enriched")));
    }

    @Test
    @DisplayName("Should pass all parameters correctly in default enrich method")
    public void shouldPassAllParametersCorrectlyInDefaultEnrichMethod() {
        // Given: Specific parameter values
        final RuntimeException throwable = new RuntimeException("Specific error");
        final ErrorResponseResource resource = new ErrorResponseResource(throwable);
        final WebRequest request = mock(WebRequest.class);
        final HttpStatus httpStatus = HttpStatus.NOT_FOUND;

        final TestErrorResponseEnricher enricher = new TestErrorResponseEnricher();

        // When: Calling default enrich method
        enricher.enrich(resource, request, httpStatus);

        // Then: All parameters are passed correctly
        assertThat(enricher.capturedResource, is(notNullValue()));
        assertThat(enricher.capturedThrowable, is(notNullValue()));
        assertThat(enricher.capturedThrowable.getMessage(), is(equalTo("Specific error")));
        assertThat(enricher.capturedRequest, is(notNullValue()));
        assertThat(enricher.capturedHttpStatus, is(equalTo(HttpStatus.NOT_FOUND)));
    }

    /**
     * Test implementation of ErrorResponseEnricher to verify method calls.
     */
    private static class TestErrorResponseEnricher implements ErrorResponseEnricher {

        private boolean wasCalled = false;
        private ErrorResponseResource capturedResource;
        private Throwable capturedThrowable;
        private WebRequest capturedRequest;
        private HttpStatus capturedHttpStatus;

        @Override
        public void doEnrich(
            final ErrorResponseResource errorResponseResource,
            final Throwable throwable,
            final WebRequest request,
            final HttpStatus httpStatus) {
            this.wasCalled = true;
            this.capturedResource = errorResponseResource;
            this.capturedThrowable = throwable;
            this.capturedRequest = request;
            this.capturedHttpStatus = httpStatus;
        }
    }
}
