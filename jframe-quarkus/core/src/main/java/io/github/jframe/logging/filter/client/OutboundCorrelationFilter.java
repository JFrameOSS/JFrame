package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.kibana.KibanaLogFields;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TRACE_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static io.github.jframe.util.constants.Constants.Headers.REQ_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TRACE_ID_HEADER;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * JAX-RS {@link ClientRequestFilter} that propagates correlation IDs from MDC to outbound HTTP headers.
 *
 * <p>Reads x-transaction-id, x-request-id and x-trace-id from {@link KibanaLogFields} (SLF4J MDC)
 * and adds them as headers on the outbound request. Existing headers are never overwritten and
 * {@code null} or blank MDC values are silently skipped.
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
        addHeaderIfAbsent(headers, TX_ID_HEADER, KibanaLogFields.get(TX_ID));
        addHeaderIfAbsent(headers, REQ_ID_HEADER, KibanaLogFields.get(REQUEST_ID));
        addHeaderIfAbsent(headers, TRACE_ID_HEADER, KibanaLogFields.get(TRACE_ID));
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
