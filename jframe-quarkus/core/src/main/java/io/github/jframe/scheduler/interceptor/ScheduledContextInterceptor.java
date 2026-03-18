package io.github.jframe.scheduler.interceptor;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;

import java.util.UUID;
import jakarta.interceptor.AroundInvoke;
import jakarta.interceptor.Interceptor;
import jakarta.interceptor.InvocationContext;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import static io.github.jframe.logging.kibana.KibanaLogFieldNames.REQUEST_ID;
import static io.github.jframe.logging.kibana.KibanaLogFieldNames.TX_ID;
import static java.util.UUID.randomUUID;

/**
 * CDI interceptor that propagates correlation context for scheduled task invocations.
 *
 * <p>Activated on methods or types annotated with {@link ScheduledWithContext}.
 * Generates a single UUID used for both the request ID and transaction ID, tags the
 * SLF4J MDC, and guarantees cleanup in a {@code finally} block.
 */
@Interceptor
@ScheduledWithContext
public class ScheduledContextInterceptor {

    private static final Logger LOG = LoggerFactory.getLogger(ScheduledContextInterceptor.class);

    /**
     * Wraps the target method invocation with correlation context.
     *
     * @param context the CDI invocation context
     * @return the return value from the intercepted method
     * @throws Exception if the intercepted method throws
     */
    @AroundInvoke
    public Object aroundInvoke(final InvocationContext context) throws Exception {
        try {
            final UUID uuid = randomUUID();
            RequestId.set(uuid);
            KibanaLogFields.tag(REQUEST_ID, RequestId.get());

            TransactionId.set(uuid);
            KibanaLogFields.tag(TX_ID, TransactionId.get());

            LOG.trace("Started scheduled task with tx id '{}'.", TransactionId.get());
            return context.proceed();
        } catch (final Exception exception) {
            LOG.error("Caught error '{}'.", exception.getMessage(), exception);
            throw exception;
        } finally {
            RequestId.remove();
            TransactionId.remove();
            KibanaLogFields.clear();
        }
    }
}
