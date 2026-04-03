package io.github.jframe.tracing.scheduled;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanBuilder;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Scope;

import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.Signature;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.SPAN_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TRACE_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingScheduledTaskEnricher}.
 *
 * <p>Verifies span creation, MDC tagging, and the save/restore pattern for scheduled task tracing:
 * <ul>
 * <li>Span is created with correct attributes (service name, method, tx_id, request_id)</li>
 * <li>MDC is tagged with trace ID and span ID during execution</li>
 * <li>AutoCloseable restores previous MDC values and ends the span</li>
 * <li>Tracer failures produce a no-op closeable without propagating exceptions</li>
 * </ul>
 */
@DisplayName("Tracing - TracingScheduledTaskEnricher")
class TracingScheduledTaskEnricherTest extends UnitTest {

    @Mock
    private Tracer tracer;
    @Mock
    private ProceedingJoinPoint joinPoint;
    @Mock
    private Signature signature;
    @Mock
    private Span span;
    @Mock
    private SpanBuilder spanBuilder;
    @Mock
    private Scope scope;
    @Mock
    private SpanContext spanContext;

    private TracingScheduledTaskEnricher enricher;

    private static final String TEST_TRACE_ID_VALUE = "aabbccdd00112233aabbccdd00112233";
    private static final String TEST_SPAN_ID_VALUE = "aabbccdd00112233";

    @Override
    @BeforeEach
    public void setUp() {
        enricher = new TracingScheduledTaskEnricher(tracer);
        setupSpanBuilderMocks();
        setupJoinPointMocks();
    }

    @Test
    @DisplayName("Should create span with correct attributes from join point")
    void enrich_shouldCreateSpanWithCorrectAttributes() throws Exception {
        // Given: MDC contains TX_ID and REQUEST_ID (set by ScheduledAspect)
        EcsFields.tag(TX_ID, "scheduled-tx-123");
        EcsFields.tag(REQUEST_ID, "scheduled-req-456");

        // When: Enricher is called
        final AutoCloseable closeable = enricher.enrich(joinPoint);

        // Then: A span is created with expected attributes
        assertThat(closeable, is(notNullValue()));
        verify(tracer).spanBuilder("Object.doWork");
        verify(spanBuilder).setAttribute("service.name", "Object");
        verify(spanBuilder).setAttribute("service.method", "doWork");
        verify(spanBuilder).setAttribute("transaction.id", "scheduled-tx-123");
        verify(spanBuilder).setAttribute("request.id", "scheduled-req-456");
        verify(spanBuilder).startSpan();
    }

    @Test
    @DisplayName("Should tag MDC with trace ID and span ID during enrichment")
    void enrich_shouldTagMdcWithTraceIdAndSpanId() throws Exception {
        // Given: No prior trace context in MDC
        EcsFields.clear(TRACE_ID, SPAN_ID);

        // When: Enricher is called
        enricher.enrich(joinPoint);

        // Then: MDC is tagged with the span's trace and span IDs
        assertThat(EcsFields.get(TRACE_ID), is(TEST_TRACE_ID_VALUE));
        assertThat(EcsFields.get(SPAN_ID), is(TEST_SPAN_ID_VALUE));
    }

    @Test
    @DisplayName("Should end span and restore previous MDC values when closeable is closed")
    void enrich_closeable_shouldEndSpanAndRestoreMdc() throws Exception {
        // Given: Previous MDC values exist (e.g., from an outer context)
        final String previousTraceId = "11111111111111111111111111111111";
        final String previousSpanId = "1111111111111111";
        EcsFields.tag(TRACE_ID, previousTraceId);
        EcsFields.tag(SPAN_ID, previousSpanId);

        final AutoCloseable closeable = enricher.enrich(joinPoint);

        // Verify MDC was overwritten during enrichment
        assertThat(EcsFields.get(TRACE_ID), is(TEST_TRACE_ID_VALUE));
        assertThat(EcsFields.get(SPAN_ID), is(TEST_SPAN_ID_VALUE));

        // When: The closeable is closed (scheduled task completed)
        closeable.close();

        // Then: Span is ended
        verify(span).end();

        // And: Previous MDC values are restored
        assertThat(EcsFields.get(TRACE_ID), is(previousTraceId));
        assertThat(EcsFields.get(SPAN_ID), is(previousSpanId));
    }

    @Test
    @DisplayName("Should clear MDC fields when no previous values existed")
    void enrich_closeable_whenNoPreviousMdc_shouldClearMdc() throws Exception {
        // Given: No previous trace context in MDC
        EcsFields.clear(TRACE_ID, SPAN_ID);

        final AutoCloseable closeable = enricher.enrich(joinPoint);

        // Verify MDC was set during enrichment
        assertThat(EcsFields.get(TRACE_ID), is(TEST_TRACE_ID_VALUE));
        assertThat(EcsFields.get(SPAN_ID), is(TEST_SPAN_ID_VALUE));

        // When: The closeable is closed
        closeable.close();

        // Then: Span is ended
        verify(span).end();

        // And: MDC fields are cleared (not left dangling)
        assertThat(EcsFields.get(TRACE_ID), is(nullValue()));
        assertThat(EcsFields.get(SPAN_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should return no-op closeable when tracer fails")
    void enrich_whenTracerFails_shouldReturnNoOpCloseable() throws Exception {
        // Given: Tracer throws during span creation
        when(tracer.spanBuilder(anyString())).thenThrow(new RuntimeException("tracer broken"));

        // When: Enricher is called
        final AutoCloseable closeable = enricher.enrich(joinPoint);

        // Then: A no-op closeable is returned (not null)
        assertThat(closeable, is(notNullValue()));

        // And: Closing it does not throw
        closeable.close();

        // And: No span was started or ended
        verify(spanBuilder, never()).startSpan();
        verify(span, never()).end();
    }

    @Test
    @DisplayName("Should set enduser.id span attribute from authenticated subject")
    void enrich_shouldSetRemoteUserAttribute() throws Exception {
        // Given: Standard setup (authenticated subject resolved by AuthenticationUtil)

        // When: Enricher is called
        enricher.enrich(joinPoint);

        // Then: The enduser.id attribute is set on the span builder
        verify(spanBuilder).setAttribute("enduser.id", "ANONYMOUS - NO AUTHENTICATION");
        verify(spanBuilder).startSpan();
    }

    private void setupSpanBuilderMocks() {
        lenient().when(tracer.spanBuilder(anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.setAttribute(anyString(), anyString())).thenReturn(spanBuilder);
        lenient().when(spanBuilder.startSpan()).thenReturn(span);
        lenient().when(span.makeCurrent()).thenReturn(scope);
        lenient().when(span.getSpanContext()).thenReturn(spanContext);
        lenient().when(spanContext.getTraceId()).thenReturn(TEST_TRACE_ID_VALUE);
        lenient().when(spanContext.getSpanId()).thenReturn(TEST_SPAN_ID_VALUE);
        lenient().when(span.setAttribute(anyString(), anyString())).thenReturn(span);
        lenient().when(span.setAttribute(anyString(), any(Boolean.class))).thenReturn(span);
    }

    private void setupJoinPointMocks() {
        lenient().when(joinPoint.getTarget()).thenReturn(new Object());
        lenient().when(joinPoint.getSignature()).thenReturn(signature);
        lenient().when(signature.getName()).thenReturn("doWork");
    }
}
