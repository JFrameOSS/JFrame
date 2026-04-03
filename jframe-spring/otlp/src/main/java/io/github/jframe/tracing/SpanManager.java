package io.github.jframe.tracing;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.security.AuthenticationConstants;
import io.opentelemetry.api.OpenTelemetry;
import io.opentelemetry.api.trace.Span;
import io.opentelemetry.api.trace.SpanKind;
import io.opentelemetry.api.trace.StatusCode;
import io.opentelemetry.api.trace.Tracer;
import io.opentelemetry.context.Context;
import io.opentelemetry.context.propagation.TextMapSetter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.net.URI;
import java.util.Optional;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpMethod;
import org.springframework.http.HttpStatusCode;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static io.github.jframe.logging.ecs.EcsFieldNames.USER_NAME;
import static io.github.jframe.util.constants.Constants.Headers.L7_REQUEST_ID;

/**
 * Manages the creation, enrichment, and finalization of spans for outbound HTTP requests.
 */
@Slf4j
@Component
@RequiredArgsConstructor
@ConditionalOnProperty(
    name = "jframe.otlp.disabled",
    havingValue = "false"
)
public class SpanManager {

    private final Tracer tracer;

    private final OpenTelemetry openTelemetry;

    /**
     * Creates a new outbound span for an HTTP request, setting various attributes such as method, URI, service name, and user information.
     *
     * @param method      the HTTP method of the request (e.g., GET, POST)
     * @param uri         the URI of the request
     * @param serviceName the name of the service being called
     * @return a new Span instance representing the outbound request
     */
    public Span createOutboundSpan(final HttpMethod method, final URI uri, final String serviceName) {
        final String host = uri.getHost();
        final String methodName = method.name();

        return tracer.spanBuilder(methodName + " " + host)
            .setSpanKind(SpanKind.CLIENT)
            .setAttribute(SPAN_PEER_SERVICE.getKey(), serviceName)
            .setAttribute(SPAN_SERVICE_NAME.getKey(), host)
            .setAttribute(SPAN_HTTP_REMOTE_USER.getKey(), EcsFields.getOrDefault(USER_NAME, AuthenticationConstants.ANONYMOUS))
            .setAttribute(SPAN_HTTP_TRANSACTION_ID.getKey(), EcsFields.get(TX_ID))
            .setAttribute(SPAN_HTTP_REQUEST_ID.getKey(), EcsFields.get(REQUEST_ID))
            .setAttribute(SPAN_EXT_REQUEST_URI.getKey(), host + uri.getPath())
            .setAttribute(SPAN_EXT_REQUEST_QUERY.getKey(), uri.getQuery())
            .setAttribute(SPAN_EXT_REQUEST_METHOD.getKey(), methodName)
            .startSpan();
    }

    /**
     * Enriches the given span with attributes from the HTTP response, including status code, content type, content length, and Layer 7
     * request ID.
     *
     * @param span       the span to enrich
     * @param statusCode the HTTP status code of the response
     * @param headers    the HTTP headers from the response
     */
    public void enrichOutboundSpan(final Span span, final HttpStatusCode statusCode, final HttpHeaders headers) {
        log.trace(
            "[Tracing] Enriching outgoing HTTP span '{}' with status code '{}'",
            span.getSpanContext().getSpanId(),
            statusCode.value()
        );
        span.setAttribute(SPAN_EXT_RESPONSE_STATUS_CODE.getKey(), statusCode.value());
        span.setAttribute(SPAN_EXT_RESPONSE_CONTENT_TYPE.getKey(), extractContentType(headers));
        span.setAttribute(SPAN_EXT_RESPONSE_CONTENT_LENGTH.getKey(), extractContentLength(headers));
        setL7RequestId(span, headers);

        if (statusCode.isError()) {
            span.setStatus(StatusCode.ERROR);
        }
    }

    /**
     * Finishes the given span, marking it as completed and logging its ID.
     *
     * @param span the span to finish
     */
    public void finishSpan(final Span span) {
        span.end();
        log.trace("[Tracing] Completed outgoing HTTP span '{}'", span.getSpanContext().getSpanId());
    }

    /**
     * Injects the current trace context into the provided HTTP headers.
     *
     * @param headers the HTTP headers to inject the trace context into
     */
    public void injectTraceContext(final HttpHeaders headers) {
        final TextMapSetter<HttpHeaders> setter = HttpHeaders::set;
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), headers, setter);
    }

    /**
     * Injects the current trace context into the provided ClientRequest.Builder.
     *
     * @param requestBuilder the ClientRequest.Builder to inject the trace context into
     */
    public void injectTraceContext(final ClientRequest.Builder requestBuilder) {
        final TextMapSetter<ClientRequest.Builder> setter = ClientRequest.Builder::header;
        openTelemetry.getPropagators().getTextMapPropagator().inject(Context.current(), requestBuilder, setter);
    }

    private String extractContentType(final HttpHeaders headers) {
        return headers.getContentType() != null
            ? headers.getContentType().toString()
            : "unknown";
    }

    private String extractContentLength(final HttpHeaders headers) {
        return headers.getContentLength() > 0
            ? String.valueOf(headers.getContentLength())
            : "-1";
    }

    private void setL7RequestId(final Span span, final HttpHeaders headers) {
        Optional.ofNullable(headers.getFirst(L7_REQUEST_ID))
            .ifPresent(requestId -> span.setAttribute(SPAN_EXT_RESPONSE_L7_REQUEST_ID.getKey(), requestId));
    }
}
