package io.github.jframe.logging.filter.type;

import java.io.IOException;
import java.util.concurrent.TimeUnit;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * JAX-RS filter that records and logs the duration of each HTTP request.
 *
 * <p>On the inbound request: stores the current nanosecond timestamp as a request property.
 * On the outbound response: retrieves the timestamp, computes the elapsed duration, and logs it.
 */
public class RequestDurationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final Logger LOG = LoggerFactory.getLogger(RequestDurationFilter.class);

    private static final String START_TIMESTAMP = "start_timestamp";

    /** Creates a new {@code RequestDurationFilter}. */
    public RequestDurationFilter() {
        // no-arg constructor
    }

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIMESTAMP, System.nanoTime());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        final Object startTimestamp = requestContext.getProperty(START_TIMESTAMP);
        if (startTimestamp instanceof Long start) {
            final long durationNanos = System.nanoTime() - start;
            final long durationMs = TimeUnit.NANOSECONDS.toMillis(durationNanos);
            LOG.debug("Request duration: {} ms", durationMs);
        }
    }
}
