package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.kibana.KibanaLogFields;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Component;
import org.springframework.web.context.request.WebRequest;

import static io.github.jframe.logging.filter.config.TransactionIdFilterConfiguration.FILTER_PREFIX;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;

/**
 * This enricher copies the http status value and text onto the error response resource.
 */
@Component
@ConditionalOnProperty(
    prefix = FILTER_PREFIX,
    name = "enabled",
    matchIfMissing = true
)
public class TransactionIdResponseEnricher implements ErrorResponseEnricher {

    /**
     * {@inheritDoc}
     *
     * <p><strong>NOTE:</strong> This enricher applies to all exceptions, whenever the TransactionIdFilter is enabled.</p>
     */
    @Override
    public void doEnrich(
        final ErrorResponseResource errorResponseResource,
        final Throwable throwable,
        final WebRequest request,
        final HttpStatus httpStatus) {
        errorResponseResource.setTxId(KibanaLogFields.get(TX_ID));
    }
}
