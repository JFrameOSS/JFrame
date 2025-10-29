package io.github.jframe.logging.filter.type;

import io.github.jframe.logging.filter.AbstractGenericFilter;
import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.TransactionId;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import java.io.IOException;
import java.util.UUID;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.apache.commons.lang3.StringUtils;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static java.util.UUID.randomUUID;

/**
 * A filter that assigns each request a unique transaction id and output the transaction id to the response header.
 */
@Slf4j
@RequiredArgsConstructor
public class TransactionIdFilter extends AbstractGenericFilter {

    /** The incoming transaction id header name. */
    private final String headerName;

    @Override
    protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain filterChain)
        throws ServletException, IOException {
        final UUID uuid = resolve(request, headerName);
        TransactionId.set(uuid);
        KibanaLogFields.tag(TX_ID, TransactionId.get());

        log.debug("Set '{}' with value '{};.", TX_ID.getLogName(), uuid);
        if (!response.containsHeader(headerName)) {
            response.addHeader(headerName, TransactionId.get());
        }

        filterChain.doFilter(request, response);
    }

    /**
     * Resolve the UUID from the header with name {@code headerName}.
     *
     * <p>If the value is not set in the {@code request} then a new UUID will be generated.
     *
     * @param request    THe request to get a UUID value from.
     * @param headerName THe header to get the UUID value from.
     * @return The resolved UUID, or a new uuid.
     */
    public static UUID resolve(final HttpServletRequest request, final String headerName) {
        UUID uuid = null;

        final String txIdHeader = request.getHeader(headerName);
        if (StringUtils.isNotBlank(txIdHeader)) {
            log.trace("Found header '{}' with value '{}' in request.", headerName, txIdHeader);
            try {
                uuid = UUID.fromString(txIdHeader);
            } catch (final IllegalArgumentException exception) {
                log.error("Could not create UUID from header.", exception);
            }
        }

        if (uuid == null) {
            uuid = randomUUID();
            log.trace("Generated new UUID '{}'.", uuid);
        }

        return uuid;
    }
}
