package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import java.util.UUID;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.core.MultivaluedMap;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;

/**
 * JAX-RS filter that generates and propagates a unique request ID for each HTTP request.
 *
 * <p>On the inbound request: generates a UUID, stores it in the {@link RequestId} ThreadLocal.
 * On the outbound response: adds the request ID to the response header (if not already present).
 */
@RequiredArgsConstructor
public class RequestIdFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private final String headerName;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        final UUID requestId = UUID.randomUUID();
        RequestId.set(requestId);
        KibanaLogFields.tag(REQUEST_ID, RequestId.get());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        final MultivaluedMap<String, Object> headers = responseContext.getHeaders();
        final String requestId = RequestId.get();
        if (requestId != null && !headers.containsKey(headerName)) {
            headers.putSingle(headerName, requestId);
        }
    }
}
