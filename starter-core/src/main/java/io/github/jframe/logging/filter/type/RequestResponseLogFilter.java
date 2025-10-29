package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.wrapper.ResettableHttpServletRequest;
import io.github.jframe.logging.wrapper.WrappedContentCachingResponse;
import io.github.jframe.logging.wrapper.WrappedHttpRequestResponse;
import lombok.RequiredArgsConstructor;

import java.io.IOException;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Filter that logs the input and output of each HTTP request. It also logs the duration of the request.
 */
@RequiredArgsConstructor
public class RequestResponseLogFilter extends AbstractGenericFilter {

    /** The request response logger to use. */
    private final RequestResponseLogger requestResponseLogger;

    /** The filter voter. */
    private final FilterVoter filterVoter;

    @Override
    protected void doFilterInternal(final HttpServletRequest httpServletRequest,
        final HttpServletResponse httpServletResponse,
        final FilterChain filterChain)
        throws ServletException, IOException {
        if (filterVoter.enabled(httpServletRequest)) {
            final WrappedHttpRequestResponse wrapped = getWrapped(httpServletRequest, httpServletResponse);
            requestResponseLogger.logRequest(wrapped.getRequest());

            try {
                filterChain.doFilter(wrapped.getRequest(), wrapped.getResponse());
            } finally {
                logResponse(wrapped);
            }
        } else {
            try {
                filterChain.doFilter(httpServletRequest, httpServletResponse);
            } finally {
                logResponse(getWrapped(httpServletRequest));
            }
        }
    }

    private void logResponse(final WrappedHttpRequestResponse wrapped) throws IOException {
        if (wrapped != null) {
            final ResettableHttpServletRequest request = wrapped.getRequest();
            if (!isAsyncStarted(request) && filterVoter.enabled(request)) {
                final WrappedContentCachingResponse response = wrapped.getResponse();
                if (response != null) {
                    requestResponseLogger.logResponse(request, response);
                }
            }
        }
    }

    @Override
    protected boolean shouldNotFilterAsyncDispatch() {
        return false;
    }
}
