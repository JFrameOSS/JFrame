package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.FilterConfig;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * JAX-RS {@link ClientRequestFilter} that propagates correlation IDs from MDC to outbound HTTP headers.
 *
 * <p>Reads x-transaction-id and x-request-id from {@link EcsFields} (SLF4J MDC)
 * and adds them as headers on the outbound request. Existing headers are never overwritten and
 * {@code null} or blank MDC values are silently skipped.
 *
 * <p>Trace context propagation is handled entirely by Quarkus's built-in OTel REST-client
 * instrumentation via the standard W3C {@code traceparent} header — no jframe code needed.
 */
@Provider
@ApplicationScoped
@Priority(100)
@Slf4j
public class OutboundCorrelationFilter implements ClientRequestFilter {

    private final FilterConfig filterConfig;

    /**
     * Creates a new {@code OutboundCorrelationFilter} with the given filter configuration.
     *
     * @param filterConfig the filter configuration used to determine whether the filter is enabled
     */
    public OutboundCorrelationFilter(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
        if (!filterConfig.outboundCorrelation().enabled()) {
            return;
        }
        final MultivaluedMap<String, Object> headers = requestContext.getHeaders();
        addHeaderIfAbsent(headers, TX_ID_HEADER, EcsFields.get(TX_ID));
        addHeaderIfAbsent(headers, REQ_ID_HEADER, EcsFields.get(REQUEST_ID));
    }

    private void addHeaderIfAbsent(
        final MultivaluedMap<String, Object> headers,
        final String headerName,
        final String value) {
        if (value != null && !headers.containsKey(headerName)) {
            headers.add(headerName, value);
        }
    }
}
