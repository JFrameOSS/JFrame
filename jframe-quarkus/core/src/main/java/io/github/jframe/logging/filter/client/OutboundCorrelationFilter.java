package io.github.jframe.logging.filter.client;

import io.github.jframe.logging.kibana.KibanaLogFields;

import java.io.IOException;
import jakarta.ws.rs.client.ClientRequestContext;
import jakarta.ws.rs.client.ClientRequestFilter;
import jakarta.ws.rs.core.MultivaluedMap;

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
public class OutboundCorrelationFilter implements ClientRequestFilter {

    @Override
    public void filter(final ClientRequestContext requestContext) throws IOException {
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
