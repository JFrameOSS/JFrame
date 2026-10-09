package io.github.jframe.exception.enricher;

import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.logging.model.TransactionId;
import io.quarkus.arc.properties.IfBuildProperty;

import jakarta.annotation.Priority;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.ws.rs.container.ContainerRequestContext;

/**
 * Enricher that sets the transaction ID from the current {@link TransactionId} ThreadLocal.
 */
@IfBuildProperty(
    name = "jframe.exception.enabled",
    stringValue = "true",
    enableIfMissing = true
)
@Priority(ErrorResponseEnricher.BUILT_IN_PRIORITY)
@ApplicationScoped
public class TransactionIdResponseEnricher implements ErrorResponseEnricher {

    @Override
    public void doEnrich(
        final ErrorResponseResource resource,
        final Throwable throwable,
        final ContainerRequestContext requestContext,
        final int statusCode) {

        resource.setTxId(TransactionId.get());
    }
}
