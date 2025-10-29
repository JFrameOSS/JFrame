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
     * The constructor.
     *
     * @param request  The HTTP servlet request.
     * @param response The HTTP servlet response.
     */
    public WrappedHttpRequestResponse(final HttpServletRequest request, final HttpServletResponse response) {
        this.response = new WrappedContentCachingResponse(response);
        this.request = new ResettableHttpServletRequest(request, response);
    }
}
