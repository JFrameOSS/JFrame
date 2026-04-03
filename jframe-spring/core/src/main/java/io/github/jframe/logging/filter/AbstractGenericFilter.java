package io.github.jframe.logging.filter;

import io.github.jframe.logging.wrapper.WrappedHttpRequestResponse;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.springframework.web.filter.OncePerRequestFilter;

import static io.github.jframe.logging.wrapper.WrappedHttpRequestResponse.WRAPPED_REQUEST_RESPONSE;

/**
 * Adapter "interface" to be able to write FilterBeans that can be "once per request" or "for every dispatch in the request" without having
 * to change code, with the additions of it being marked by {@link MockMvcFilter}. The marker helps to identify useful filters for testing
 * purposes. So, if a filter changes from {@link AbstractGenericFilter} to {@link OncePerRequestFilter} then the filter code remains the
 * same.
 */
public abstract class AbstractGenericFilter extends OncePerRequestFilter implements MockMvcFilter {

    /**
     * Retrieve the {@link WrappedHttpRequestResponse} given the {@code httpServletRequest}.
     *
     * @param httpServletRequest The http servlet request.
     * @return The, possibly {@code null}, {@link WrappedHttpRequestResponse}.
     */
    protected WrappedHttpRequestResponse getWrapped(final HttpServletRequest httpServletRequest) {
        return (WrappedHttpRequestResponse) httpServletRequest.getAttribute(WRAPPED_REQUEST_RESPONSE);
    }

    /**
     * Retrieve or create the {@link WrappedHttpRequestResponse}.
     *
     * @param request  The http servlet request.
     * @param response The http servlet response.
     * @return a, never {@code null} {@link WrappedHttpRequestResponse}.
     */
    protected WrappedHttpRequestResponse getWrapped(final HttpServletRequest request, final HttpServletResponse response) {
        WrappedHttpRequestResponse wrapped = getWrapped(request);
        if (wrapped == null) {
            wrapped = new WrappedHttpRequestResponse(request, response);
            request.setAttribute(WRAPPED_REQUEST_RESPONSE, wrapped);
        }
        return wrapped;
    }
}
