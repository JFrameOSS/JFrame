package io.github.jframe.exception.handler.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.model.TransactionId;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static io.github.jframe.logging.filter.config.TransactionIdFilterConfiguration.FILTER_PREFIX;

/**
 * This enricher copies the http status value and text onto the error response resource.
 */
@Order(ErrorResponseEnricher.BUILT_IN_ORDER + 20)
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
        final String txId = TransactionId.get();
        errorResponseResource.setTxId(txId == null ? EcsFields.get(TX_ID) : txId);
    }
}
