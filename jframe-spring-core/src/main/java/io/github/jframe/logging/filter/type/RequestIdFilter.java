package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static java.util.UUID.randomUUID;

/**
 * A filter that assigns each request a unique request id and output the request id to the response header.
 */
@Slf4j
@RequiredArgsConstructor
public class RequestIdFilter extends AbstractGenericFilter {

    /** The incoming request id header name. */
    private final String headerName;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        final UUID uuid = randomUUID();

        RequestId.set(uuid);
        KibanaLogFields.tag(REQUEST_ID, RequestId.get());

        log.debug("Set '{}' with value '{};.", REQUEST_ID.getLogName(), uuid);

        if (!response.containsHeader(headerName)) {
            response.addHeader(headerName, RequestId.get());
        }

        filterChain.doFilter(request, response);
    }
}
