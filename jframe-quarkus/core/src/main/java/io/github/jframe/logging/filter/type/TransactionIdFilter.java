package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.model.TransactionId;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;
import jakarta.ws.rs.ext.Provider;

import org.apache.commons.lang3.StringUtils;

import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;

/**
 * JAX-RS filter that resolves or generates a transaction ID for each HTTP request.
 *
 * <p>On the inbound request: reads the transaction ID header; if a valid UUID is found it is
 * re-used, otherwise a new UUID is generated. The result is stored in the
 * {@link TransactionId} ThreadLocal.
 * On the outbound response: adds the transaction ID to the response header (if not already present).
 */
@Provider
@ApplicationScoped
@Priority(100)
@Slf4j
public class TransactionIdFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private final FilterConfig filterConfig;

    /**
     * Creates a new {@code TransactionIdFilter} with the given filter configuration.
     *
     * @param filterConfig the filter configuration used to determine whether the filter is enabled
     */
    public TransactionIdFilter(final FilterConfig filterConfig) {
        this.filterConfig = filterConfig;
    }

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
        if (!filterConfig.transactionId().enabled()) {
            return;
        }
        final String headerValue = requestContext.getHeaderString(TX_ID_HEADER);
        final UUID transactionId = resolve(headerValue);
        TransactionId.set(transactionId);
        EcsFields.tag(TX_ID, TransactionId.get());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (!filterConfig.transactionId().enabled()) {
            return;
        }
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        final String transactionId = TransactionId.get();
        if (transactionId != null && !headers.containsKey(TX_ID_HEADER)) {
            headers.putSingle(TX_ID_HEADER, transactionId);
        }
    }
}
