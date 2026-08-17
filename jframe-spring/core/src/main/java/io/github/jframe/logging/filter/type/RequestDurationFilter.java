package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.ecs.AutoCloseableEcsField;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.github.jframe.logging.voter.FilterVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static io.github.jframe.logging.ecs.EcsFieldNames.*;
import static io.github.jframe.logging.ecs.EcsFields.tagCloseable;
import static io.github.jframe.logging.ecs.LogTypeNames.END;

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

        try (AutoCloseableEcsField closableTag = tagCloseable(LOG_TYPE, END)) {
            log.debug("Found tag '{}':'{}' [{}].", LOG_TYPE, END, closableTag);
            final String duration = formatDurationMillis(System.nanoTime() - start);
            EcsFields.tag(TX_DURATION, duration);
            EcsFields.tag(REQUEST_DURATION, duration);
            log.debug("Duration '{}' ms.", duration);
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }

    /**
     * Formats a nanosecond duration as a millisecond value with exactly 2 decimal places,
     * always using {@code '.'} as the decimal separator (locale-invariant).
     *
     * <p>Equivalent to {@code String.format("%.2f", nanos / 1E6)} but without locale machinery
     * and without per-call {@code Formatter} allocation.
     *
     * @param nanos elapsed nanoseconds
     * @return duration string, e.g. {@code "12.34"}
     */
    static String formatDurationMillis(final long nanos) {
        final long hundredths = Math.round(nanos / 10_000.0);
        final long whole = hundredths / 100;
        final long fraction = Math.abs(hundredths % 100);
        return whole + "." + (fraction < 10 ? "0" : "") + fraction;
    }
}
