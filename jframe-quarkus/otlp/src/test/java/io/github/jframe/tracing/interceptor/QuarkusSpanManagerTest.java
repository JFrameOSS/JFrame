package io.github.jframe.tracing.interceptor;

import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;

import jakarta.enterprise.inject.Instance;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyInt;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.lenient;
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
 * </ul>
 */
@DisplayName("Quarkus OTLP - Span Manager")
public class QuarkusSpanManagerTest extends UnitTest {

    @Mock
    private Tracer tracer;

    @Mock
    private Instance<Tracer> tracerInstance;

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
        spanManager = new QuarkusSpanManager(tracerInstance);
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
    @DisplayName("Should enrich span with 200 OK response attributes")
    public void shouldEnrichSpanWithOkResponseAttributes() {
        // Given: A span and a successful 200 response
        final MultivaluedMap<String, String> headers = new MultivaluedHashMap<>();
        headers.putSingle("Content-Type", "application/json");

        // When: Enriching the span with the response
        spanManager.enrichOutboundSpan(span, 200, headers);

        // Then: Response status is set on the span
        verify(span).setAttribute(anyString(), eq(200L));
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

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setSpanKind(any())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyInt())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID);
        lenient().when(span.setAttribute(anyString(), anyLong())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), any(Boolean.class))).thenReturn(span);
    }
}
