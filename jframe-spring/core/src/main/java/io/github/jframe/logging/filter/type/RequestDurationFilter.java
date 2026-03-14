package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.github.jframe.logging.kibana.AutoCloseableKibanaLogField;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.voter.FilterVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.*;
import static io.github.jframe.logging.kibana.KibanaLogFields.tagCloseable;
import static io.github.jframe.logging.kibana.KibanaLogTypeNames.END;

/**
 * A filter that logs the duration of the request.
 */
@Slf4j
@RequiredArgsConstructor
public class RequestDurationFilter extends AbstractGenericFilter {

    /** The request attribute name for the start timestamp. */
    private static final String START_TIMESTAMP = "start_timestamp";

    /** The filter voter. */
    private final FilterVoter filterVoter;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        if (request.getAttribute(START_TIMESTAMP) == null) {
            request.setAttribute(START_TIMESTAMP, System.nanoTime());
        }

        try {
            filterChain.doFilter(request, response);
        } finally {
            logEnd(request);
        }
    }

    private void logEnd(final HttpServletRequest request) {
        if (!isAsyncStarted(request) && filterVoter.enabled(request)) {
            logEnd((Long) request.getAttribute(START_TIMESTAMP));
        }
    }

    private static void logEnd(final Long start) {
        if (start == null) {
            log.info("Could not read start timestamp from request!");
            return;
        }

        try (AutoCloseableKibanaLogField closableTag = tagCloseable(LOG_TYPE, END)) {
            log.debug("Found tag '{}':'{}' [{}].", LOG_TYPE, END, closableTag);
            final String duration = String.format("%.2f", (System.nanoTime() - start) / 1E6);
            KibanaLogFields.tag(TX_DURATION, duration);
            KibanaLogFields.tag(REQUEST_DURATION, duration);
            log.info("Duration '{}' ms.", duration);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
