package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.FilterConfig;
import io.github.support.UnitTest;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanContext;
import io.opentelemetry.api.trace.TraceFlags;
import io.opentelemetry.api.trace.TraceState;

import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.core.MultivaluedHashMap;
import jakarta.ws.rs.core.MultivaluedMap;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;
import org.mockito.MockedStatic;
import org.mockito.junit.jupiter.MockitoSettings;
import org.mockito.quality.Strictness;

import static io.github.jframe.logging.ecs.EcsFieldNames.SPAN_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TRACE_ID;
import static io.github.jframe.util.constants.Constants.Headers.SPAN_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.lenient;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link TracingResponseFilter} (Quarkus JAX-RS OTLP filter).
 *
 * <p>Verifies the TracingResponseFilter functionality including:
 * <ul>
 * <li>Trace/span ID extraction from OpenTelemetry {@link Span#current()} and population into MDC</li>
 * <li>Propagation of trace and span IDs as HTTP response headers (x-trace-id, x-span-id)</li>
 * <li>MDC cleanup via EcsFields after the response phase (finally block)</li>
 * <li>Graceful handling of invalid / noop span contexts (no headers, no exception)</li>
 * <li>Early-exit when the filter is disabled via {@link FilterConfig}</li>
 * <li>No duplication of headers already present on the response</li>
 * </ul>
 */
@MockitoSettings(strictness = Strictness.LENIENT)
@DisplayName("Quarkus OTLP Logging Filters - Tracing Response Filter")
public class TracingResponseFilterTest extends UnitTest {

    // ======================== CONSTANTS ========================

    private static final String TEST_TRACE_ID = "0123456789abcdef0123456789abcdef";
    private static final String TEST_SPAN_ID = "0123456789abcdef";

    // ======================== FIXTURES ========================

    @Mock
    private FilterConfig filterConfig;

    private boolean tracingEnabled = true;

    private TracingResponseFilter filter;

    @Override
    @BeforeEach
    public void setUp() {
        tracingEnabled = true;
        // Wire config: filterConfig.tracingResponse().enabled() drives early-exit logic
        lenient().when(filterConfig.tracingResponse()).thenAnswer(inv -> (FilterConfig.TracingResponseConfig) () -> tracingEnabled);
        filter = new TracingResponseFilter(filterConfig);
    }

    @AfterEach
    public void tearDown() {
        // Prevent MDC pollution across tests
        EcsFields.clear();
    }

    // ======================== FACTORY METHODS ========================

    private Span aValidSpan() {
        final SpanContext validContext = SpanContext.create(
            TEST_TRACE_ID,
            TEST_SPAN_ID,
            TraceFlags.getSampled(),
            TraceState.getDefault()
        );
        final Span span = mock(Span.class);
        when(span.getSpanContext()).thenReturn(validContext);
        return span;
    }

    private Span anInvalidSpan() {
        final Span span = mock(Span.class);
        when(span.getSpanContext()).thenReturn(SpanContext.getInvalid());
        return span;
    }

    private MultivaluedMap<String, Object> emptyResponseHeaders() {
        return new MultivaluedHashMap<>();
    }

    // ======================== AC1+AC2: REQUEST PHASE — MDC POPULATION ========================

    @Nested
    @DisplayName("AC1+AC2 - Request phase: MDC population from valid span")
    class RequestPhaseMdcPopulation {

        @Test
        @DisplayName("Should populate MDC TRACE_ID when span context is valid")
        public void shouldPopulateMdcTraceIdWhenSpanContextIsValid() throws Exception {
            // Given: A valid span is current
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: TRACE_ID is stored in MDC
                final String traceId = EcsFields.get(TRACE_ID);
                assertThat(traceId, is(TEST_TRACE_ID));
            }
        }

        @Test
        @DisplayName("Should populate MDC SPAN_ID when span context is valid")
        public void shouldPopulateMdcSpanIdWhenSpanContextIsValid() throws Exception {
            // Given: A valid span is current
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: SPAN_ID is stored in MDC
                final String spanId = EcsFields.get(SPAN_ID);
                assertThat(spanId, is(TEST_SPAN_ID));
            }
        }

        @Test
        @DisplayName("Should populate both TRACE_ID and SPAN_ID in MDC from sampled span")
        public void shouldPopulateBothTraceIdAndSpanIdInMdcFromSampledSpan() throws Exception {
            // Given: A valid sampled span is current
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: Both MDC fields are populated
                assertThat(EcsFields.get(TRACE_ID), is(TEST_TRACE_ID));
                assertThat(EcsFields.get(SPAN_ID), is(TEST_SPAN_ID));
            }
        }
    }

    // ======================== AC1: RESPONSE PHASE — HEADERS ADDED ========================


    @Nested
    @DisplayName("AC1 - Response phase: trace/span headers added to response")
    class ResponsePhaseHeaders {

        @Test
        @DisplayName("Should add x-trace-id header to response when MDC has TRACE_ID")
        public void shouldAddTraceIdHeaderToResponseWhenMdcHasTraceId() throws Exception {
            // Given: TRACE_ID and SPAN_ID are already populated in MDC (simulating request phase)
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: x-trace-id header is present in the response
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
            assertThat(headers.getFirst(TRACE_ID_HEADER), is(TEST_TRACE_ID));
        }

        @Test
        @DisplayName("Should add x-span-id header to response when MDC has SPAN_ID")
        public void shouldAddSpanIdHeaderToResponseWhenMdcHasSpanId() throws Exception {
            // Given: TRACE_ID and SPAN_ID are already populated in MDC (simulating request phase)
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: x-span-id header is present in the response
            assertThat(headers.containsKey(SPAN_ID_HEADER), is(true));
            assertThat(headers.getFirst(SPAN_ID_HEADER), is(TEST_SPAN_ID));
        }

        @Test
        @DisplayName("Should add both x-trace-id and x-span-id headers in a full request/response cycle")
        public void shouldAddBothHeadersInFullRequestResponseCycle() throws Exception {
            // Given: A valid span is current during the request phase
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);

                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
                final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
                final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
                when(responseContext.getHeaders()).thenReturn(headers);

                // When: Full filter cycle — request then response
                filter.filter(requestContext);
                filter.filter(requestContext, responseContext);

                // Then: Both headers are set on the response
                assertThat(headers.containsKey(TRACE_ID_HEADER), is(true));
                assertThat(headers.containsKey(SPAN_ID_HEADER), is(true));
                assertThat(headers.getFirst(TRACE_ID_HEADER), is(TEST_TRACE_ID));
                assertThat(headers.getFirst(SPAN_ID_HEADER), is(TEST_SPAN_ID));
            }
        }
    }

    // ======================== AC3+AC4: INVALID / NOOP SPAN ========================


    @Nested
    @DisplayName("AC3+AC4 - Invalid/noop span: graceful handling, no headers, no error")
    class InvalidSpanHandling {

        @Test
        @DisplayName("Should not populate MDC when span context is invalid")
        public void shouldNotPopulateMdcWhenSpanContextIsInvalid() throws Exception {
            // Given: An invalid (noop) span is current
            final Span invalidSpan = anInvalidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(invalidSpan);
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: MDC fields remain empty — no trace/span IDs set
                assertThat(EcsFields.get(TRACE_ID), is(nullValue()));
                assertThat(EcsFields.get(SPAN_ID), is(nullValue()));
            }
        }

        @Test
        @DisplayName("Should not add response headers when span context is invalid")
        public void shouldNotAddResponseHeadersWhenSpanContextIsInvalid() throws Exception {
            // Given: An invalid span was current during request — MDC was NOT populated
            final Span invalidSpan = anInvalidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(invalidSpan);

                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
                final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
                final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
                when(responseContext.getHeaders()).thenReturn(headers);

                // When: Full filter cycle with invalid span
                filter.filter(requestContext);
                filter.filter(requestContext, responseContext);

                // Then: No trace/span headers added to the response
                assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
                assertThat(headers.containsKey(SPAN_ID_HEADER), is(false));
            }
        }

        @Test
        @DisplayName("Should not throw any exception when span context is invalid")
        public void shouldNotThrowAnyExceptionWhenSpanContextIsInvalid() throws Exception {
            // Given: An invalid span is current (e.g. no active trace)
            final Span invalidSpan = anInvalidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(invalidSpan);

                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
                final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
                final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
                when(responseContext.getHeaders()).thenReturn(headers);

                // When & Then: Both filter phases complete without any exception
                filter.filter(requestContext);
                filter.filter(requestContext, responseContext);
                // Reaching here means no exception was thrown — test passes
            }
        }

        @Test
        @DisplayName("Should not throw any exception when Span.current() returns Span.getInvalid()")
        public void shouldNotThrowAnyExceptionWhenSpanCurrentReturnsGetInvalid() throws Exception {
            // Given: Retrieve the noop instance BEFORE mocking static to avoid UnfinishedStubbingException
            final Span noopSpan = Span.getInvalid();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(noopSpan);

                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When & Then: Request filter completes without exception
                filter.filter(requestContext);
                // Reaching here means no exception — test passes
            }
        }
    }

    // ======================== AC5: DISABLED VIA CONFIG ========================


    @Nested
    @DisplayName("AC5 - Disabled via config: skip everything")
    class DisabledViaConfig {

        @Test
        @DisplayName("Should not populate MDC when filter is disabled")
        public void shouldNotPopulateMdcWhenFilterIsDisabled() throws Exception {
            // Given: Filter is disabled via config
            tracingEnabled = false;
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: MDC is not populated — filter was skipped
                assertThat(EcsFields.get(TRACE_ID), is(nullValue()));
                assertThat(EcsFields.get(SPAN_ID), is(nullValue()));
            }
        }

        @Test
        @DisplayName("Should not add response headers when filter is disabled")
        public void shouldNotAddResponseHeadersWhenFilterIsDisabled() throws Exception {
            // Given: Filter is disabled via config and MDC has some values pre-set
            tracingEnabled = false;
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: No trace/span headers are added — filter was skipped
            assertThat(headers.containsKey(TRACE_ID_HEADER), is(false));
            assertThat(headers.containsKey(SPAN_ID_HEADER), is(false));
        }

        @Test
        @DisplayName("Should not consult Span.current() when filter is disabled during request phase")
        public void shouldNotConsultSpanCurrentWhenFilterIsDisabledDuringRequestPhase() throws Exception {
            // Given: Filter is disabled via config
            tracingEnabled = false;

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);

                // When: Filter processes the incoming request
                filter.filter(requestContext);

                // Then: Span.current() is never called — early return before any OTEL interaction
                spanMock.verify(Span::current, org.mockito.Mockito.never());
            }
        }
    }

    // ======================== AC6: MDC CLEARED AFTER RESPONSE ========================


    @Nested
    @DisplayName("AC6 - MDC cleared after response filter (finally block)")
    class MdcClearedAfterResponse {

        @Test
        @DisplayName("Should clear TRACE_ID from MDC after response filter completes")
        public void shouldClearTraceIdFromMdcAfterResponseFilterCompletes() throws Exception {
            // Given: TRACE_ID and SPAN_ID are in MDC (set by the request phase)
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: TRACE_ID is cleared from MDC in the finally block
            assertThat(EcsFields.get(TRACE_ID), is(nullValue()));
        }

        @Test
        @DisplayName("Should clear SPAN_ID from MDC after response filter completes")
        public void shouldClearSpanIdFromMdcAfterResponseFilterCompletes() throws Exception {
            // Given: TRACE_ID and SPAN_ID are in MDC (set by the request phase)
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: SPAN_ID is cleared from MDC in the finally block
            assertThat(EcsFields.get(SPAN_ID), is(nullValue()));
        }

        @Test
        @DisplayName("Should clear both TRACE_ID and SPAN_ID from MDC after full request/response cycle")
        public void shouldClearBothFieldsFromMdcAfterFullRequestResponseCycle() throws Exception {
            // Given: A valid span is current during the request phase
            final Span validSpan = aValidSpan();

            try (MockedStatic<Span> spanMock = mockStatic(Span.class)) {
                spanMock.when(Span::current).thenReturn(validSpan);

                final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
                final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
                final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
                when(responseContext.getHeaders()).thenReturn(headers);

                // When: Full filter cycle — request then response
                filter.filter(requestContext);
                filter.filter(requestContext, responseContext);

                // Then: Both MDC fields are cleared after the response phase
                assertThat(EcsFields.get(TRACE_ID), is(nullValue()));
                assertThat(EcsFields.get(SPAN_ID), is(nullValue()));
            }
        }
    }

    // ======================== AC8: HEADERS NOT DUPLICATED ========================


    @Nested
    @DisplayName("AC8 - Headers not duplicated if already present in response")
    class HeadersNotDuplicated {

        @Test
        @DisplayName("Should not overwrite x-trace-id header if already present in response")
        public void shouldNotOverwriteTraceIdHeaderIfAlreadyPresentInResponse() throws Exception {
            // Given: Response already has x-trace-id set (e.g. upstream proxy added it)
            final String existingTraceId = "existing-trace-id-value";
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            headers.putSingle(TRACE_ID_HEADER, existingTraceId);
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: Header count remains 1 — original value is not duplicated
            assertThat(headers.get(TRACE_ID_HEADER).size(), is(1));
        }

        @Test
        @DisplayName("Should not overwrite x-span-id header if already present in response")
        public void shouldNotOverwriteSpanIdHeaderIfAlreadyPresentInResponse() throws Exception {
            // Given: Response already has x-span-id set
            final String existingSpanId = "existing-span-id-value";
            EcsFields.tag(TRACE_ID, TEST_TRACE_ID);
            EcsFields.tag(SPAN_ID, TEST_SPAN_ID);

            final ContainerRequestContext requestContext = mock(ContainerRequestContext.class);
            final ContainerResponseContext responseContext = mock(ContainerResponseContext.class);
            final MultivaluedMap<String, Object> headers = emptyResponseHeaders();
            headers.putSingle(SPAN_ID_HEADER, existingSpanId);
            when(responseContext.getHeaders()).thenReturn(headers);

            // When: Filter processes the outgoing response
            filter.filter(requestContext, responseContext);

            // Then: Header count remains 1 — original value is not duplicated
            assertThat(headers.get(SPAN_ID_HEADER).size(), is(1));
        }
    }
}
