package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.TransactionId;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.UUID;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import org.apache.commons.lang3.StringUtils;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;

/**
 * JAX-RS filter that resolves or generates a transaction ID for each HTTP request.
 *
 * <p>On the inbound request: reads the transaction ID header; if a valid UUID is found it is
 * re-used, otherwise a new UUID is generated. The result is stored in the
 * {@link TransactionId} ThreadLocal.
 * On the outbound response: adds the transaction ID to the response header (if not already present).
 */
@RequiredArgsConstructor
public class TransactionIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final String headerName;

    /**
     * Resolves a {@link UUID} from the supplied header value string.
     *
     * <p>Returns a freshly generated UUID when {@code headerValue} is {@code null}, blank,
     * or not a valid UUID string.
     *
     * @param headerValue the raw header value, may be {@code null}
     * @return the resolved or newly generated {@link UUID}
     */
    public static UUID resolve(final String headerValue) {
        if (StringUtils.isBlank(headerValue)) {
            return UUID.randomUUID();
        }
        return parseOrGenerate(headerValue);
    }

    private static UUID parseOrGenerate(final String headerValue) {
        try {
            return UUID.fromString(headerValue.trim());
        } catch (final IllegalArgumentException e) {
            return UUID.randomUUID();
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final String headerValue = requestContext.getHeaderString(headerName);
        final UUID transactionId = resolve(headerValue);
        TransactionId.set(transactionId);
        KibanaLogFields.tag(TX_ID, TransactionId.get());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        final String transactionId = TransactionId.get();
        if (transactionId != null && !headers.containsKey(headerName)) {
            headers.putSingle(headerName, transactionId);
        }
    }
}
