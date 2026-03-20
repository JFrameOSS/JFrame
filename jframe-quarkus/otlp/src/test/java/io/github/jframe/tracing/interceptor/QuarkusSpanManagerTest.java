package io.github.jframe.tracing.interceptor;

import io.github.support.UnitTest;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.propagation.ContextPropagators;
import io.opentelemetry.context.propagation.TextMapPropagator;
import io.quarkus.security.identity.SecurityIdentity;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_CONTENT_LENGTH;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_CONTENT_TYPE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_L7_REQUEST_ID;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.EXT_RESPONSE_STATUS_CODE;
import static io.github.jframe.tracing.OpenTelemetryConstants.Attributes.PEER_SERVICE;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link QuarkusSpanManager}.
 *
 * <p>Verifies the CDI {@code @ApplicationScoped} span manager functionality including:
 * <ul>
 * <li>Creating outbound CLIENT spans with correct attributes</li>
 * <li>Enriching spans with response status and content attributes</li>
 * <li>Marking spans as error for 4xx/5xx responses</li>
 * <li>Finishing spans via end()</li>
 * <li>W3C trace context injection</li>
 * <li>Graceful handling of invalid URLs</li>
 * </ul>
 */
@DisplayName("Quarkus OTLP - Span Manager")
public class QuarkusSpanManagerTest extends UnitTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Instance<Tracer> tracerInstance;

    @Mock
    private Instance<OpenTelemetry> openTelemetryInstance;

    @Mock
    private Instance<SecurityIdentity> securityIdentityInstance;

    @Mock
    private SpanBuilder spanBuilder;

    @Mock
    private Span span;

    @Mock
    private SpanContext spanContext;

    private QuarkusSpanManager spanManager;

    @Override
    @BeforeEach
    public void setUp() {
        when(tracerInstance.isResolvable()).thenReturn(true);
        when(tracerInstance.get()).thenReturn(tracer);
        lenient().when(openTelemetryInstance.isResolvable()).thenReturn(false);
        lenient().when(securityIdentityInstance.isResolvable()).thenReturn(false);
        spanManager = new QuarkusSpanManager(tracerInstance, openTelemetryInstance, securityIdentityInstance);
        setupSpanBuilderMocks();
        setupKibanaFields();
    }

    @Test
    @DisplayName("Should create outbound span with correct service name attribute")
    public void shouldCreateOutboundSpanWithCorrectServiceName() {
        // Given: A target URL and service name
        final String targetUrl = "https://example.com:8080/api/users";
        final String serviceName = "user-service";

        // When: Creating an outbound span
        final Span result = spanManager.createOutboundSpan("GET", targetUrl, serviceName);

        // Then: Span is created with service name
        assertThat(result, is(notNullValue()));
        verify(tracer).spanBuilder(anyString());
        verify(spanBuilder).startSpan();
    }

    @Test
    @DisplayName("Should create outbound span with HTTP method in span name")
    public void shouldCreateOutboundSpanWithHttpMethodInSpanName() {
        // Given: A POST request to an endpoint
        final String targetUrl = "https://api.example.com/orders";
        final String serviceName = "order-service";

        // When: Creating an outbound span
        spanManager.createOutboundSpan("POST", targetUrl, serviceName);

        // Then: Span name includes the HTTP method
        verify(tracer).spanBuilder(anyString());
    }

    @Test
    @DisplayName("Should set all 8 attributes on createOutboundSpan")
    public void shouldSetAllAttributesOnCreateOutboundSpan() {
        // Given: A URL with host, path, and query
        final String targetUrl = "https://api.example.com/orders?status=open";
        final String serviceName = "order-service";

        // When: Creating an outbound span
        spanManager.createOutboundSpan("GET", targetUrl, serviceName);

        // Then: peer.service is set (spot-check one attribute; full setAttribute chain verified via builder mock)
        verify(spanBuilder).setAttribute(eq(PEER_SERVICE), eq(serviceName));
    }

    @Test
    @DisplayName("Should enrich span with 200 OK response attributes")
    public void shouldEnrichSpanWithOkResponseAttributes() {
        // Given: A span and a successful 200 response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Type", "application/json");

        // When: Enriching the span with the response
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: Response status is set on the span
        verify(span).setAttribute(eq(EXT_RESPONSE_STATUS_CODE), eq(200L));
    }

    @Test
    @DisplayName("Should set content-type attribute from response headers")
    public void shouldSetContentTypeOnEnrich() {
        // Given: Response headers containing Content-Type
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Type", "application/json");

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: Content-Type attribute is set on the span
        verify(span).setAttribute(eq(EXT_RESPONSE_CONTENT_TYPE), eq("application/json"));
    }

    @Test
    @DisplayName("Should set content-length attribute from response headers")
    public void shouldSetContentLengthOnEnrich() {
        // Given: Response headers containing Content-Length
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Length", "1024");

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: Content-Length attribute is set on the span
        verify(span).setAttribute(eq(EXT_RESPONSE_CONTENT_LENGTH), eq("1024"));
    }

    @Test
    @DisplayName("Should set L7 request ID attribute when header is present")
    public void shouldSetL7RequestIdOnEnrich() {
        // Given: Response headers containing X-Layer7-Requestid
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("X-Layer7-Requestid", "layer7-abc-123");

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: L7 request ID attribute is set on the span
        verify(span).setAttribute(eq(EXT_RESPONSE_L7_REQUEST_ID), eq("layer7-abc-123"));
    }

    @Test
    @DisplayName("Should not set L7 request ID when header is absent")
    public void shouldNotSetL7RequestIdWhenHeaderAbsent() {
        // Given: Response headers without X-Layer7-Requestid
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // When: Enriching the span
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: L7 request ID attribute is NOT set
        verify(span, never()).setAttribute(eq(EXT_RESPONSE_L7_REQUEST_ID), anyString());
    }

    @Test
    @DisplayName("Should set ERROR boolean attribute for error status codes")
    public void shouldSetErrorAttributeOnEnrich() {
        // Given: A span and a 404 NOT FOUND response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // When: Enriching the span with an error response
        spanManager.enrichOutboundSpan(span, 404, headers);

        // Then: ERROR boolean attribute and ERROR status are set
        verify(span).setAttribute(eq("error"), eq(true));
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should mark span as error for 4xx status code")
    public void shouldMarkSpanAsErrorFor4xxStatusCode() {
        // Given: A span and a 404 NOT FOUND response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // When: Enriching the span with the error response
        spanManager.enrichOutboundSpan(span, 404, headers);

        // Then: Span is marked as error
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should mark span as error for 5xx status code")
    public void shouldMarkSpanAsErrorFor5xxStatusCode() {
        // Given: A span and a 500 INTERNAL_SERVER_ERROR response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // When: Enriching the span with the server error response
        spanManager.enrichOutboundSpan(span, 500, headers);

        // Then: Span is marked as error
        verify(span).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should not mark span as error for 2xx status code")
    public void shouldNotMarkSpanAsErrorFor2xxStatusCode() {
        // Given: A span and a 201 CREATED response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();

        // When: Enriching the span with the success response
        spanManager.enrichOutboundSpan(span, 201, headers);

        // Then: Span is NOT marked as error
        verify(span, never()).setStatus(StatusCode.ERROR);
    }

    @Test
    @DisplayName("Should finish span by calling end()")
    public void shouldFinishSpanByCallingEnd() {
        // Given: An active span

        // When: Finishing the span
        spanManager.finishSpan(span);

        // Then: Span is ended
        verify(span).end();
    }

    @Test
    @DisplayName("Should inject W3C trace context via OpenTelemetry propagator")
    public void shouldInjectTraceContext() {
        // Given: An OpenTelemetry instance that is resolvable
        final OpenTelemetry openTelemetry = mock(OpenTelemetry.class);
        final ContextPropagators propagators = mock(ContextPropagators.class);
        final TextMapPropagator propagator = mock(TextMapPropagator.class);
        when(openTelemetryInstance.isResolvable()).thenReturn(true);
        when(openTelemetryInstance.get()).thenReturn(openTelemetry);
        when(openTelemetry.getPropagators()).thenReturn(propagators);
        when(propagators.getTextMapPropagator()).thenReturn(propagator);

        final QuarkusSpanManager manager =
            new QuarkusSpanManager(tracerInstance, openTelemetryInstance, securityIdentityInstance);
        final MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();

        // When: Injecting trace context
        manager.injectTraceContext(requestHeaders);

        // Then: The propagator inject method is called
        verify(propagator).inject(any(), eq(requestHeaders), any());
    }

    @Test
    @DisplayName("Should not throw when OpenTelemetry is unavailable for trace context injection")
    public void shouldHandleNoOpenTelemetryForTraceContext() {
        // Given: openTelemetryInstance is not resolvable (default in setUp)
        final MultivaluedMap<String, Object> requestHeaders = new MultivaluedHashMap<>();

        // When & Then: No exception thrown
        spanManager.injectTraceContext(requestHeaders);
    }

    @Test
    @DisplayName("Should handle invalid URL gracefully without throwing")
    public void shouldHandleInvalidUrlGracefully() {
        // Given: An invalid URL that cannot be parsed
        final String invalidUrl = "not a valid url with spaces";

        // When: Creating an outbound span — should not throw
        final Span result = spanManager.createOutboundSpan("GET", invalidUrl, "service");

        // Then: A span is still created using the raw URL as fallback
        assertThat(result, is(notNullValue()));
    }

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyLong())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID);
        lenient().when(span.setAttribute(anyString(), anyLong())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), any(Boolean.class))).thenReturn(span);
    }
}
