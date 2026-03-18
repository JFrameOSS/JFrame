package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.wrapper.CachingRequestContext;
import io.github.jframe.logging.wrapper.CachingResponseContext;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

/**
 * JAX-RS filter that logs full request and response details including body capture and masking.
 *
 * <p>Uses {@link FilterVoter} to decide whether logging is enabled for a given request.
 * Delegates actual logging to {@link RequestResponseLogger}.
 * Cleans up ThreadLocal and MDC fields in the response phase.
 */
@RequiredArgsConstructor
public class RequestResponseLogFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private static final String CACHING_REQUEST_PROPERTY =
        "io.github.jframe.logging.filter.type.RequestResponseLogFilter.CACHING_REQUEST";

    private final RequestResponseLogger requestResponseLogger;
    private final FilterVoter filterVoter;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (filterVoter.enabled(requestContext)) {
            final CachingRequestContext cachingRequest = new CachingRequestContext(requestContext);
            requestContext.setProperty(CACHING_REQUEST_PROPERTY, cachingRequest);
            requestResponseLogger.logRequest(cachingRequest);
        }
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        try {
            if (filterVoter.enabled(requestContext)) {
                final CachingResponseContext cachingResponse = new CachingResponseContext(responseContext);
                requestResponseLogger.logResponse(requestContext, cachingResponse);
            }
        } finally {
            TransactionId.remove();
            RequestId.remove();
            KibanaLogFields.clear();
        }
    }
}
