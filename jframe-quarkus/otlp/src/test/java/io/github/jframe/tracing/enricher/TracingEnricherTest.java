package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;
import io.quarkus.security.identity.SecurityIdentity;

import java.net.URI;
import java.security.Principal;
import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.MediaType;
import jakarta.ws.rs.core.UriInfo;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_MESSAGE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.ERROR_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_METHOD;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_QUERY;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REMOTE_USER;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_TRANSACTION_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.HTTP_URI;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.anyBoolean;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link TracingEnricher}.
 *
 * <p>Verifies the enricher correctly populates tracing context on both the error response resource
 * and the active OpenTelemetry span, including:
 * <ul>
 * <li>traceId and spanId are set on the resource from the active span</li>
 * <li>Span is marked as ERROR with error attributes</li>
 * <li>HTTP context attributes are set on the span</li>
 * <li>Correlation IDs (TX_ID, REQUEST_ID) are recorded on the span</li>
 * <li>Graceful handling when no active span exists (null span)</li>
 * <li>Graceful handling when span context is invalid</li>
 * <li>Graceful handling when throwable message is null</li>
 * <li>Graceful handling when requestContext.getUriInfo() is null</li>
 * <li>Graceful handling when requestContext.getMediaType() is null</li>
 * </ul>
 */
@DisplayName("Unit Test - Tracing Enricher")
public class TracingEnricherTest extends UnitTest {

    private TracingEnricher enricher;

    @BeforeEach
    @Override
    public void setUp() {
        enricher = new TracingEnricher();
    }

    // ─── Happy path: valid span ──────────────────────────────────────────────

    @Test
    @DisplayName("Should set traceId and spanId on resource when valid span exists")
    public void shouldSetTraceIdAndSpanIdOnResourceWhenValidSpanExists() {
        // Given: A valid span and a populated request context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("something went wrong");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/users", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: The trace and span IDs are set on the resource
            assertThat(resource.getTraceId(), is(equalTo(TEST_TRACE_ID)));
            assertThat(resource.getSpanId(), is(equalTo(TEST_SPAN_ID)));
        }
    }

    @Test
    @DisplayName("Should set span status to ERROR when valid span exists")
    public void shouldSetSpanStatusToErrorWhenValidSpanExists() {
        // Given: A valid span and a request context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("something went wrong");
            final ContainerRequestContext requestContext = aRequestContext("POST", "/api/orders", MediaType.APPLICATION_JSON_TYPE);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Span status is set to ERROR
            verify(span).setStatus(StatusCode.ERROR);
        }
    }

    @Test
    @DisplayName("Should set error attributes on span when valid span exists")
    public void shouldSetErrorAttributesOnSpanWhenValidSpanExists() {
        // Given: A valid span and a throwable with a known message
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new IllegalArgumentException("bad argument");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/items", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 400);

            // Then: Error attributes are set on the span
            verify(span).setAttribute(ERROR, true);
            verify(span).setAttribute(ERROR_TYPE, "IllegalArgumentException");
            verify(span).setAttribute(ERROR_MESSAGE, "bad argument");
        }
    }

    @Test
    @DisplayName("Should set HTTP context attributes on span when valid span exists")
    public void shouldSetHttpContextAttributesOnSpanWhenValidSpanExists() {
        // Given: A valid span and a request context with full HTTP details
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("DELETE", "/api/users/42", MediaType.APPLICATION_JSON_TYPE);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 404);

            // Then: HTTP context attributes are set on the span
            verify(span).setAttribute(HTTP_URI, "/api/users/42");
            verify(span).setAttribute(HTTP_METHOD, "DELETE");
            verify(span).setAttribute(HTTP_STATUS_CODE, (long) 404);
            verify(span).setAttribute(HTTP_CONTENT_TYPE, "application/json");
        }
    }

    @Test
    @DisplayName("Should set correlation IDs on span when KibanaLogFields are populated")
    public void shouldSetCorrelationIdsOnSpanWhenKibanaLogFieldsArePopulated() {
        // Given: A valid span and KibanaLogFields populated with TX_ID and REQUEST_ID
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);
            setupKibanaFields();

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/health", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Correlation IDs are set on the span from KibanaLogFields
            verify(span).setAttribute(HTTP_TRANSACTION_ID, TEST_TX_ID);
            verify(span).setAttribute(HTTP_REQUEST_ID, TEST_REQUEST_ID);
        }
    }

    @Test
    @DisplayName("Should set null correlation IDs when KibanaLogFields are not populated")
    public void shouldSetNullCorrelationIdsWhenKibanaLogFieldsAreNotPopulated() {
        // Given: A valid span but KibanaLogFields are empty (no TX_ID or REQUEST_ID)
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);
            // KibanaLogFields deliberately NOT populated

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/health", null);

            // When: Enriching the resource (should not throw NPE)
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Correlation ID attributes are still set (with null values from KibanaLogFields)
            verify(span).setAttribute(HTTP_TRANSACTION_ID, KibanaLogFields.get(TX_ID));
            verify(span).setAttribute(HTTP_REQUEST_ID, KibanaLogFields.get(REQUEST_ID));
        }
    }

    // ─── Null throwable message ──────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null throwable message gracefully")
    public void shouldHandleNullThrowableMessageGracefully() {
        // Given: A valid span and a throwable with null message
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new NullPointerException(); // null message
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/test", null);

            // When: Enriching the resource — should not throw NPE
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Error type is set and error message is null (handled gracefully)
            verify(span).setAttribute(ERROR_TYPE, "NullPointerException");
            verify(span).setAttribute(ERROR_MESSAGE, (String) null);
        }
    }

    // ─── Null content type ───────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null media type gracefully")
    public void shouldHandleNullMediaTypeGracefully() {
        // Given: A valid span and a request context with null media type
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final UriInfo uriInfo = mock(UriInfo.class);

            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getUriInfo()).thenReturn(uriInfo);
            when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/test"));
            when(requestContext.getMediaType()).thenReturn(null);

            // When: Enriching the resource — should not throw NPE
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: HTTP_CONTENT_TYPE is set with null (handled gracefully)
            verify(span).setAttribute(HTTP_CONTENT_TYPE, (String) null);
        }
    }

    // ─── Null uriInfo ────────────────────────────────────────────────────────

    @Test
    @DisplayName("Should handle null uriInfo gracefully without NPE")
    public void shouldHandleNullUriInfoGracefullyWithoutNpe() {
        // Given: A valid span and a request context where getUriInfo() returns null
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getUriInfo()).thenReturn(null);
            when(requestContext.getMediaType()).thenReturn(null);

            // When: Enriching the resource — should not throw NPE
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Resource still has traceId/spanId set (no NPE thrown)
            assertThat(resource.getTraceId(), is(equalTo(TEST_TRACE_ID)));
            assertThat(resource.getSpanId(), is(equalTo(TEST_SPAN_ID)));
        }
    }

    // ─── Graceful handling: null span ────────────────────────────────────────

    @Test
    @DisplayName("Should not enrich resource when Span.current() returns null")
    public void shouldNotEnrichResourceWhenSpanCurrentReturnsNull() {
        // Given: No active span (Span.current() returns null)
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            spanMock.when(Span::current).thenReturn(null);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/test", null);

            // When: Enriching the resource — should not throw NPE
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Resource fields remain unset
            assertThat(resource.getTraceId(), is(nullValue()));
            assertThat(resource.getSpanId(), is(nullValue()));
        }
    }

    @Test
    @DisplayName("Should not set span attributes when Span.current() returns null")
    public void shouldNotSetSpanAttributesWhenSpanCurrentReturnsNull() {
        // Given: No active span (Span.current() returns null)
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span nullSpanProxy = mock(Span.class);
            spanMock.when(Span::current).thenReturn(null);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/test", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: The proxy span was never used
            verify(nullSpanProxy, never()).setStatus(StatusCode.ERROR);
        }
    }

    // ─── Graceful handling: invalid span context ─────────────────────────────

    @Test
    @DisplayName("Should not enrich resource when span context is invalid")
    public void shouldNotEnrichResourceWhenSpanContextIsInvalid() {
        // Given: An active span with an invalid span context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = anInvalidSpan();
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/test", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: Resource fields remain unset
            assertThat(resource.getTraceId(), is(nullValue()));
            assertThat(resource.getSpanId(), is(nullValue()));
        }
    }

    @Test
    @DisplayName("Should not set span attributes when span context is invalid")
    public void shouldNotSetSpanAttributesWhenSpanContextIsInvalid() {
        // Given: An active span with an invalid span context
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = anInvalidSpan();
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/test", null);

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: No span status or attributes are set
            verify(span, never()).setStatus(StatusCode.ERROR);
            verify(span, never()).setAttribute(anyString(), anyString());
            verify(span, never()).setAttribute(anyString(), anyBoolean());
            verify(span, never()).setAttribute(anyString(), anyLong());
        }
    }

    // ─── Default interface method (enrich) ──────────────────────────────────

    @Test
    @DisplayName("Should delegate to doEnrich when default enrich method is called")
    public void shouldDelegateToDoEnrichWhenDefaultEnrichMethodIsCalled() {
        // Given: A valid span and an error resource with a throwable
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final Throwable throwable = new RuntimeException("delegate test");
            final ErrorResponseResource resource = new ErrorResponseResource(throwable);
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/delegate", null);

            // When: Calling the default enrich() method (uses resource.getThrowable())
            enricher.enrich(resource, requestContext, 500);

            // Then: Trace and span IDs are set (doEnrich was invoked)
            assertThat(resource.getTraceId(), is(equalTo(TEST_TRACE_ID)));
            assertThat(resource.getSpanId(), is(equalTo(TEST_SPAN_ID)));
        }
    }

    // ─── HTTP_REMOTE_USER: authenticated user ────────────────────────────────

    @Test
    @DisplayName("Should set HTTP_REMOTE_USER attribute when authenticated user is present")
    public void shouldSetHttpRemoteUserAttributeWhenAuthenticatedUserIsPresent() {
        // Given: A valid span and an authenticated security identity
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final SecurityIdentity identity = mock(SecurityIdentity.class);
            final Principal principal = mock(Principal.class);
            when(identity.isAnonymous()).thenReturn(false);
            when(identity.getPrincipal()).thenReturn(principal);
            when(principal.getName()).thenReturn("john.doe");

            @SuppressWarnings("unchecked") final Instance<SecurityIdentity> identityInstance = mock(Instance.class);
            when(identityInstance.isResolvable()).thenReturn(true);
            when(identityInstance.get()).thenReturn(identity);

            final TracingEnricher enricherWithSecurity = new TracingEnricher(identityInstance);
            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/users", null);

            // When: Enriching with an authenticated user
            enricherWithSecurity.doEnrich(resource, throwable, requestContext, 500);

            // Then: HTTP_REMOTE_USER is set to the principal name
            verify(span).setAttribute(HTTP_REMOTE_USER, "john.doe");
        }
    }

    @Test
    @DisplayName("Should set HTTP_REMOTE_USER to ANONYMOUS when security identity is not resolvable")
    public void shouldSetHttpRemoteUserToAnonymousWhenSecurityIdentityIsNotResolvable() {
        // Given: A valid span and no security identity (null from no-arg constructor)
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");
            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/users", null);

            // When: Enriching with no security identity (no-arg enricher)
            enricher.doEnrich(resource, throwable, requestContext, 500);

            // Then: HTTP_REMOTE_USER is set to the anonymous sentinel
            verify(span).setAttribute(HTTP_REMOTE_USER, "ANONYMOUS - NO AUTHENTICATION");
        }
    }

    // ─── HTTP_QUERY: query string ─────────────────────────────────────────────

    @Test
    @DisplayName("Should set HTTP_QUERY attribute when query string is present")
    public void shouldSetHttpQueryAttributeWhenQueryStringIsPresent() {
        // Given: A valid span and a request context with a query string
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final UriInfo uriInfo = mock(UriInfo.class);
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getUriInfo()).thenReturn(uriInfo);
            when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost/api/users?name=john&role=admin"));
            when(requestContext.getMediaType()).thenReturn(null);

            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 400);

            // Then: HTTP_QUERY attribute is set with the query string
            verify(span).setAttribute(HTTP_QUERY, "name=john&role=admin");
        }
    }

    @Test
    @DisplayName("Should NOT set HTTP_QUERY attribute when query string is null")
    public void shouldNotSetHttpQueryAttributeWhenQueryStringIsNull() {
        // Given: A valid span and a request context without a query string
        try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
            final Span span = aValidSpan(TEST_TRACE_ID, TEST_SPAN_ID);
            spanMock.when(Span::current).thenReturn(span);

            final ContainerRequestContext requestContext = aRequestContext("GET", "/api/users", null);
            final ErrorResponseResource resource = new ErrorResponseResource();
            final Throwable throwable = new RuntimeException("error");

            // When: Enriching the resource
            enricher.doEnrich(resource, throwable, requestContext, 400);

            // Then: HTTP_QUERY attribute is NOT set (null query is skipped)
            verify(span, never()).setAttribute(HTTP_QUERY, (String) null);
        }
    }

    // ─── HTTP_CONTENT_LENGTH: not implemented ────────────────────────────────
    // NOTE: HTTP_CONTENT_LENGTH is intentionally not set by this enricher.
    // The doEnrich contract only provides ContainerRequestContext (inbound request),
    // not the response context. Content-length is a response attribute and is not
    // available at error-enrichment time without buffering the entire response.

    // ─── Factory / helper methods ────────────────────────────────────────────

    private Span aValidSpan(final String traceId, final String spanId) {
        final Span span = mock(Span.class);
        final SpanContext spanContext = SpanContext.create(
            traceId,
            spanId,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );
        when(span.getSpanContext()).thenReturn(spanContext);
        // setAttribute returns span (fluent API) — lenient to avoid unnecessary-stubbing errors
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), anyLong())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), anyBoolean())).thenReturn(span);
        return span;
    }

    private Span anInvalidSpan() {
        final Span span = mock(Span.class);
        when(span.getSpanContext()).thenReturn(SpanContext.getInvalid());
        return span;
    }

    private ContainerRequestContext aRequestContext(
        final String method,
        final String path,
        final MediaType mediaType) {

        final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
        final UriInfo uriInfo = mock(UriInfo.class);

        when(requestContext.getMethod()).thenReturn(method);
        when(requestContext.getUriInfo()).thenReturn(uriInfo);
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost" + path));
        when(requestContext.getMediaType()).thenReturn(mediaType);

        return requestContext;
    }
}
