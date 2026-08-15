package io.github.jframe.logging.wrapper;

import lombok.Getter;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

/**
 * Container for wrapped HTTP {@code request} and {@code response}.
 */
@Getter
public class WrappedHttpRequestResponse {

    public static final String WRAPPED_REQUEST_RESPONSE = WrappedHttpRequestResponse.class.getName();

    private final ResettableHttpServletRequest request;

    private final WrappedContentCachingResponse response;

    /**
     * Cap-aware constructor. Both the request and response wrappers apply
     * {@code responseLength} as their logging cap while still delivering the
     * complete bodies to the application.
     *
     * @param request        The HTTP servlet request.
     * @param response       The HTTP servlet response.
     * @param responseLength Maximum bytes to retain in the logging copy ({@code -1} = unlimited).
     */
    public WrappedHttpRequestResponse(final HttpServletRequest request, final HttpServletResponse response, final int responseLength) {
        this.response = new WrappedContentCachingResponse(response, responseLength);
        this.request = new ResettableHttpServletRequest(request, response);
    }
}
