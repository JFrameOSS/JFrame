package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.kibana.AutoCloseableKibanaLogField;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.kibana.KibanaLogTypeNames;
import io.github.jframe.logging.voter.FilterVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.LOG_TYPE;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_DURATION;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_DURATION;

/**
 * JAX-RS filter that records and logs the duration of each HTTP request.
 *
 * <p>On the inbound request: stores the current nanosecond timestamp as a request property.
 * On the outbound response: retrieves the timestamp, computes the elapsed duration, and logs it.
 * Uses {@link FilterVoter} to determine whether duration logging is enabled for a given request.
 */
@RequiredArgsConstructor
@Slf4j
public class RequestDurationFilter implements ContainerRequestFilter, ContainerResponseFilter {

    private static final String START_TIMESTAMP = "start_timestamp";

    private final FilterVoter filterVoter;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        requestContext.setProperty(START_TIMESTAMP, System.nanoTime());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (filterVoter.enabled(requestContext)) {
            logEnd(requestContext);
        }
    }

    private void logEnd(final ContainerRequestContext requestContext) {
        final Object startTimestamp = requestContext.getProperty(START_TIMESTAMP);
        if (startTimestamp instanceof Long start) {
            try (AutoCloseableKibanaLogField closableTag = KibanaLogFields.tagCloseable(LOG_TYPE, KibanaLogTypeNames.END)) {
                final String duration = String.format("%.2f", (System.nanoTime() - start) / 1E6);
                KibanaLogFields.tag(TX_DURATION, duration);
                KibanaLogFields.tag(REQUEST_DURATION, duration);
                log.debug("Found tag '{}':'{}' [{}].", LOG_TYPE, KibanaLogTypeNames.END, closableTag);
                log.info("Duration '{}' ms.", duration);
            }
        }
    }
}
