package io.github.jframe.tracing;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.util.HttpBodyUtil;
import io.github.jframe.logging.wrapper.BufferedClientHttpResponse;
import io.opentelemetry.api.trace.Span;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import reactor.core.publisher.Mono;

import java.util.Optional;

import org.slf4j.MDC;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpRequest;
import org.springframework.http.client.ClientHttpRequestInterceptor;
import org.springframework.stereotype.Component;
import org.springframework.web.reactive.function.client.ClientRequest;
import org.springframework.web.reactive.function.client.ClientResponse;
import org.springframework.web.reactive.function.client.ExchangeFilterFunction;

import static io.github.jframe.OpenTelemetryConstants.Attributes.TRACING_SPAN;
import static io.github.jframe.logging.HttpLogger.*;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
import static io.github.jframe.util.constants.Constants.Headers.*;
import static java.util.Objects.nonNull;

/**
 * A filter for logging and tracing HTTP requests and responses in a Spring application.
 */
@Slf4j
@Component
@RequiredArgsConstructor
public class HttpFilter {

    private final Optional<SpanManager> spanManager;
    private final PasswordMasker passwordMasker;

    @Value("${jframe.logging.response-length}")
    private int responseLength;

    /**
     * Returns a ClientHttpRequestInterceptor that processes requests and responses for logging and tracing.
     *
     * @param serviceName the name of the service being called
     * @return a ClientHttpRequestInterceptor for logging and tracing HTTP requests and responses
     */
    public ClientHttpRequestInterceptor getRequestInterceptor(final String serviceName) {
        log.debug("Creating request interceptor for service: '{}'", serviceName);
        return (request, body, execution) -> {
            log.debug("Processing Interceptor with MDC context: {}", MDC.getCopyOfContextMap());
            request.getHeaders().add(REQ_ID_HEADER, KibanaLogFields.get(REQUEST_ID));
            request.getHeaders().add(TX_ID_HEADER, KibanaLogFields.get(TX_ID));
            request.getHeaders().add(TRACE_ID_HEADER, KibanaLogFields.get(TRACE_ID));
            logRequest(request.getMethod(), request.getURI(), request.getHeaders());

            spanManager.ifPresent(sm -> sm.injectTraceContext(request.getHeaders()));
            final Span span = spanManager
                .map(sm -> sm.createOutboundSpan(request.getMethod(), request.getURI(), serviceName))
                .orElse(null);

            try {
                final BufferedClientHttpResponse bufferedResponse = new BufferedClientHttpResponse(execution.execute(request, body));
                final String maskedBody = HttpBodyUtil.compressAndMaskBody(
                    bufferedResponse.getBodyAsString(),
                    responseLength,
                    passwordMasker
                );
                logResponse(bufferedResponse.getStatusCode(), bufferedResponse.getHeaders(), maskedBody);
                if (nonNull(span)) {
                    spanManager.get().enrichOutboundSpan(span, bufferedResponse.getStatusCode(), bufferedResponse.getHeaders());
                }

                return bufferedResponse;
            } finally {
                if (nonNull(span)) {
                    spanManager.get().finishSpan(span);
                }
            }
        };
    }

    /**
     * Returns an ExchangeFilterFunction that processes requests and responses for logging and tracing.
     *
     * @param serviceName the name of the service being called
     * @return an ExchangeFilterFunction for logging and tracing HTTP requests and responses
     */
    public ExchangeFilterFunction getExchangeFilter(final String serviceName) {
        log.debug("Creating exchange filter for service: '{}'", serviceName);
        return ExchangeFilterFunction
            .ofRequestProcessor(request -> processRequest(request, serviceName))
            .andThen(ExchangeFilterFunction.ofResponseProcessor(this::processResponse));
    }

    private Mono<ClientRequest> processRequest(final ClientRequest request, final String serviceName) {
        log.debug("Processing Exchange Filter with MDC context: {}", MDC.getCopyOfContextMap());
        final ClientRequest.Builder builder = ClientRequest.from(request)
            .header(REQ_ID_HEADER, KibanaLogFields.get(REQUEST_ID))
            .header(TX_ID_HEADER, KibanaLogFields.get(TX_ID))
            .header(TRACE_ID_HEADER, KibanaLogFields.get(TRACE_ID));

        spanManager.ifPresent(sm -> sm.injectTraceContext(builder));
        spanManager.ifPresent(sm -> {
            final Span span = sm.createOutboundSpan(request.method(), request.url(), serviceName);
            builder.attribute(TRACING_SPAN, span);
        });

        final ClientRequest enrichedRequest = builder.build();
        logRequest(enrichedRequest.method(), enrichedRequest.url(), enrichedRequest.headers());
        return Mono.just(enrichedRequest);
    }

    private Mono<ClientResponse> processResponse(final ClientResponse response) {
        final HttpRequest request = response.request();
        tagKibanaFields(request.getHeaders());
        spanManager.ifPresent(sm -> {
            final Span span = (Span) request.getAttributes().get(TRACING_SPAN);
            if (nonNull(span)) {
                try {
                    sm.enrichOutboundSpan(span, response.statusCode(), response.headers().asHttpHeaders());
                } finally {
                    sm.finishSpan(span);
                }
            }
        });

        return logReactiveResponse(response, responseLength, passwordMasker);
    }

    private void tagKibanaFields(final HttpHeaders headers) {
        KibanaLogFields.tag(TX_ID, headers.getFirst(TX_ID_HEADER));
        KibanaLogFields.tag(REQUEST_ID, headers.getFirst(REQ_ID_HEADER));
        KibanaLogFields.tag(TRACE_ID, headers.getFirst(TRACE_ID_HEADER));
    }
}
