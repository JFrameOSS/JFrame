package io.github.jframe.tracing.filter;

import io.github.jframe.logging.filter.otlp.TracingFilterConfig;
import io.github.jframe.tracing.OpenTelemetryConfig;
import io.github.jframe.tracing.interceptor.QuarkusSpanManager;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

import java.net.URI;
import java.util.Set;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link OutboundTracingFilter}.
 *
 * <p>Verifies both the {@link jakarta.ws.rs.client.ClientRequestFilter} and
 * {@link jakarta.ws.rs.client.ClientResponseFilter} halves of the outbound tracing filter,
 * including:
 * <ul>
 * <li>Creating a CLIENT span via {@link QuarkusSpanManager#createOutboundSpan(String, String, String)}</li>
 * <li>Storing the span as a property on {@link ClientRequestContext} for the response filter</li>
 * <li>Injecting W3C trace context via {@link QuarkusSpanManager#injectTraceContext(MultivaluedMap)}</li>
 * <li>Adding correlation headers (TX_ID, REQ_ID, TRACE_ID) to outbound requests</li>
 * <li>Skipping span creation when tracing is disabled</li>
 * <li>Skipping span creation when the URI path matches an excluded segment</li>
 * <li>Enriching the span with the response status code</li>
 * <li>Finishing the span in a finally block, even if enrichment throws</li>
 * <li>Handling a missing span property gracefully in the response filter</li>
 * </ul>
 */
@DisplayName("Unit Test - Outbound Tracing Filter")
public class OutboundTracingFilterTest extends UnitTest {

    @Mock
    private QuarkusSpanManager spanManager;

    @Mock
    private OpenTelemetryConfig openTelemetryConfig;

    @Mock
    private TracingFilterConfig tracingFilterConfig;

    @Mock
    private TracingFilterConfig.OutboundTracingConfig outboundTracingConfig;

    @Override
    @BeforeEach
    public void setUp() {
        // Default: tracing is enabled with no excluded paths
        lenient().when(openTelemetryConfig.disabled()).thenReturn(false);
        lenient().when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health", "actuator", "ping", "status"));
        lenient().when(tracingFilterConfig.outboundTracing()).thenReturn(outboundTracingConfig);
        lenient().when(outboundTracingConfig.enabled()).thenReturn(true);
    }

    // ─── Request filter: span creation ───────────────────────────────────────

    @Nested
    @DisplayName("Request filter")
    class RequestFilter {

        @Test
        @DisplayName("Should create outbound CLIENT span when tracing is enabled")
        public void shouldCreateOutboundClientSpanWhenTracingIsEnabled() throws Exception {
            // Given: Tracing is enabled and the path is not excluded
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/api/users");
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: Span manager creates an outbound span for the request
            verify(spanManager).createOutboundSpan(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should store span as property on request context")
        public void shouldStoreSpanAsPropertyOnRequestContext() throws Exception {
            // Given: Tracing is enabled and span creation succeeds
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/orders"));
            when(requestContext.getMethod()).thenReturn("POST");
            when(requestContext.getHeaders()).thenReturn(new MultivaluedHashMap<>());
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: The created span is stored on the request context for retrieval in the response filter
            verify(requestContext).setProperty(anyString(), any(Span.class));
        }

        @Test
        @DisplayName("Should call spanManager.injectTraceContext to propagate W3C trace context")
        public void shouldCallSpanManagerInjectTraceContextToPropagateW3cTraceContext() throws Exception {
            // Given: Tracing is enabled and a valid span is created
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/data"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(headers);
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: W3C trace context injection is delegated to SpanManager
            verify(spanManager).injectTraceContext(headers);
        }

        @Test
        @DisplayName("Should add correlation headers when KibanaLogFields are populated")
        public void shouldAddCorrelationHeadersWhenKibanaLogFieldsArePopulated() throws Exception {
            // Given: Tracing is enabled, span exists, and KibanaLogFields have TX_ID and REQUEST_ID
            setupKibanaFields();
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/data"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(headers);
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: Correlation headers are added from KibanaLogFields
            assertThat(headers.containsKey(TX_ID_HEADER), is(true));
            assertThat(headers.containsKey(REQ_ID_HEADER), is(true));
        }

        @Test
        @DisplayName("Should add TRACE_ID header from span context when span is present")
        public void shouldAddTraceIdHeaderFromSpanContextWhenSpanIsPresent() throws Exception {
            // Given: Tracing is enabled and a valid span with a known trace ID is created
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/data"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(headers);
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: TRACE_ID header is added from the span context
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
        }

        @Test
        @DisplayName("Should NOT add correlation headers when KibanaLogFields are not populated")
        public void shouldNotAddCorrelationHeadersWhenKibanaLogFieldsAreNotPopulated() throws Exception {
            // Given: Tracing is enabled but KibanaLogFields are empty (TX_ID and REQUEST_ID not set)
            final MultivaluedMap<String, Object> headers = new MultivaluedHashMap<>();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getUri()).thenReturn(URI.create("https://api.example.com/api/data"));
            when(requestContext.getMethod()).thenReturn("GET");
            when(requestContext.getHeaders()).thenReturn(headers);
            final Span span = aValidSpan();
            when(spanManager.createOutboundSpan(anyString(), anyString(), anyString())).thenReturn(span);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request (no KibanaLogFields set)
            filter.filter(requestContext);

            // Then: TX_ID and REQ_ID correlation headers are NOT added
            assertThat(headers.containsKey(TX_ID_HEADER), is(false));
            assertThat(headers.containsKey(REQ_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should NOT create span when tracing is disabled")
        public void shouldNotCreateSpanWhenTracingIsDisabled() throws Exception {
            // Given: Tracing is globally disabled
            when(openTelemetryConfig.disabled()).thenReturn(true);
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/api/users");
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: No span is created
            verify(spanManager, never()).createOutboundSpan(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should NOT create span when URI path matches an excluded segment")
        public void shouldNotCreateSpanWhenUriPathMatchesExcludedSegment() throws Exception {
            // Given: Tracing is enabled but the path contains an excluded segment ("health")
            when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health", "actuator", "ping"));
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/health");
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request to an excluded path
            filter.filter(requestContext);

            // Then: No span is created for excluded paths
            verify(spanManager, never()).createOutboundSpan(anyString(), anyString(), anyString());
        }

        @Test
        @DisplayName("Should NOT create span when URI path contains an excluded segment in a deeper path")
        public void shouldNotCreateSpanWhenUriPathContainsExcludedSegmentInDeeperPath() throws Exception {
            // Given: Tracing is enabled but the deep path contains "actuator"
            when(openTelemetryConfig.excludedMethods()).thenReturn(Set.of("health", "actuator"));
            final ClientRequestContext requestContext = aClientRequestContext("GET", "https://api.example.com/actuator/metrics");
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the outbound request
            filter.filter(requestContext);

            // Then: No span is created for paths containing an excluded segment
            verify(spanManager, never()).createOutboundSpan(anyString(), anyString(), anyString());
        }
    }

    // ─── Response filter: span enrichment and finishing ───────────────────────


    @Nested
    @DisplayName("Response filter")
    class ResponseFilter {

        @Test
        @DisplayName("Should enrich span with response status code")
        public void shouldEnrichSpanWithResponseStatusCode() throws Exception {
            // Given: A span was stored on the request context by the request filter
            final Span span = aValidSpan();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(span);
            final ClientResponseContext responseContext = aClientResponseContext(200);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the incoming response
            filter.filter(requestContext, responseContext);

            // Then: Span manager enriches the span with the response status
            verify(spanManager).enrichOutboundSpan(any(Span.class), any(Integer.class), any());
        }

        @Test
        @DisplayName("Should finish span after enrichment with 200 OK response")
        public void shouldFinishSpanAfterEnrichmentWith200OkResponse() throws Exception {
            // Given: A valid span stored on the request context
            final Span span = aValidSpan();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(span);
            final ClientResponseContext responseContext = aClientResponseContext(200);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the incoming response
            filter.filter(requestContext, responseContext);

            // Then: Span is always finished
            verify(spanManager).finishSpan(span);
        }

        @Test
        @DisplayName("Should finish span even when enrichment throws an exception")
        public void shouldFinishSpanEvenWhenEnrichmentThrowsException() throws Exception {
            // Given: Span enrichment throws an unexpected runtime exception
            final Span span = aValidSpan();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(span);
            final ClientResponseContext responseContext = aClientResponseContext(500);
            doThrow(new RuntimeException("enrichment failure"))
                .when(spanManager).enrichOutboundSpan(any(Span.class), any(Integer.class), any());
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the response (enrichment will throw)
            // Then: The exception is swallowed/handled and finishSpan is still called
            try {
                filter.filter(requestContext, responseContext);
            } catch (final Exception ignored) {
                // exception may propagate; what matters is span.end() is called
            }

            verify(spanManager).finishSpan(span);
        }

        @Test
        @DisplayName("Should handle missing span gracefully without NPE")
        public void shouldHandleMissingSpanGracefullyWithoutNpe() throws Exception {
            // Given: No span was stored on the request context (request filter was skipped)
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(null);
            final ClientResponseContext responseContext = aClientResponseContext(200);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the response without a stored span
            filter.filter(requestContext, responseContext);

            // Then: No NPE is thrown and span manager methods are NOT called
            verify(spanManager, never()).enrichOutboundSpan(any(), any(Integer.class), any());
            verify(spanManager, never()).finishSpan(any());
        }

        @Test
        @DisplayName("Should enrich span with 400 client error status code")
        public void shouldEnrichSpanWith400ClientErrorStatusCode() throws Exception {
            // Given: A valid span and a 404 client error response
            final Span span = aValidSpan();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(span);
            final ClientResponseContext responseContext = aClientResponseContext(404);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the 404 response
            filter.filter(requestContext, responseContext);

            // Then: Span is enriched with 404 and then finished
            verify(spanManager).enrichOutboundSpan(span, 404, responseContext.getHeaders());
            verify(spanManager).finishSpan(span);
        }

        @Test
        @DisplayName("Should enrich span with 500 server error status code")
        public void shouldEnrichSpanWith500ServerErrorStatusCode() throws Exception {
            // Given: A valid span and a 500 server error response
            final Span span = aValidSpan();
            final ClientRequestContext requestContext = mock(ClientRequestContext.class);
            when(requestContext.getProperty(anyString())).thenReturn(span);
            final ClientResponseContext responseContext = aClientResponseContext(500);
            final OutboundTracingFilter filter = new OutboundTracingFilter(spanManager, openTelemetryConfig, tracingFilterConfig);

            // When: Filter processes the 500 response
            filter.filter(requestContext, responseContext);

            // Then: Span is enriched with 500 and then finished
            verify(spanManager).enrichOutboundSpan(span, 500, responseContext.getHeaders());
            verify(spanManager).finishSpan(span);
        }
    }

    // ─── Factory / helper methods ─────────────────────────────────────────────

    private Span aValidSpan() {
        final Span span = mock(Span.class);
        final SpanContext spanContext = SpanContext.create(
            TEST_TRACE_ID,
            TEST_SPAN_ID,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        return span;
    }

    private ClientRequestContext aClientRequestContext(final String method, final String uri) {
        final ClientRequestContext context = mock(ClientRequestContext.class);
        when(context.getUri()).thenReturn(URI.create(uri));
        when(context.getMethod()).thenReturn(method);
        when(context.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return context;
    }

    private ClientResponseContext aClientResponseContext(final int statusCode) {
        final ClientResponseContext context = mock(ClientResponseContext.class);
        when(context.getStatus()).thenReturn(statusCode);
        when(context.getHeaders()).thenReturn(new MultivaluedHashMap<>());
        return context;
    }
}
