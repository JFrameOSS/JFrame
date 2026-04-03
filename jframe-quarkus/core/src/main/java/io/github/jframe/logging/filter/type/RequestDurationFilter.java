package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.AutoCloseableEcsField;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.ecs.LogTypeNames;
import io.github.jframe.logging.filter.FilterConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.github.jframe.logging.voter.FilterVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.container.ContainerRequestFilter;
import jakarta.ws.rs.container.ContainerResponseContext;
import jakarta.ws.rs.container.ContainerResponseFilter;
import jakarta.ws.rs.ext.Provider;

import static io.github.jframe.logging.ecs.EcsFieldNames.LOG_TYPE;
import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_DURATION;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_DURATION;

/**
 * JAX-RS filter that records and logs the duration of each HTTP request.
 *
 * <p>On the inbound request: stores the current nanosecond timestamp as a request property.
 * On the outbound response: retrieves the timestamp, computes the elapsed duration, and logs it.
 * Uses {@link FilterVoter} to determine whether duration logging is enabled for a given request.
 */
@Provider
@ApplicationScoped
@Priority(300)
@RequiredArgsConstructor
@Slf4j
public class RequestDurationFilter implements ContainerRequestFilter, ContainerResponseFilter, JFrameFilter {

    private static final String START_TIMESTAMP = "start_timestamp";

    private final FilterVoter filterVoter;
    private final FilterConfig filterConfig;

    @Override
    public void filter(final ContainerRequestContext requestContext) throws IOException {
        if (!filterConfig.requestDuration().enabled()) {
            return;
        }
        requestContext.setProperty(START_TIMESTAMP, System.nanoTime());
    }

    @Override
    public void filter(final ContainerRequestContext requestContext,
        final ContainerResponseContext responseContext) throws IOException {
        if (!filterConfig.requestDuration().enabled()) {
            return;
        }
        if (filterVoter.enabled(requestContext)) {
            logEnd(requestContext);
        }
    }

    private void logEnd(final ContainerRequestContext requestContext) {
        final Object startTimestamp = requestContext.getProperty(START_TIMESTAMP);
        if (startTimestamp instanceof Long start) {
            try (AutoCloseableEcsField closableTag = EcsFields.tagCloseable(LOG_TYPE, LogTypeNames.END)) {
                final String duration = String.format("%.2f", (System.nanoTime() - start) / 1E6);
                EcsFields.tag(TX_DURATION, duration);
                EcsFields.tag(REQUEST_DURATION, duration);
                log.debug("Found tag '{}':'{}' [{}].", LOG_TYPE, LogTypeNames.END, closableTag);
                log.info("Duration '{}' ms.", duration);
            }
        }
    }
}
