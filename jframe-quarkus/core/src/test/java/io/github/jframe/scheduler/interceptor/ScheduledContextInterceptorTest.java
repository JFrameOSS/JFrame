package io.github.jframe.scheduler.interceptor;

import io.github.jframe.logging.kibana.KibanaLogFields;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import io.github.support.UnitTest;

import jakarta.interceptor.InvocationContext;

import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.slf4j.MDC;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link ScheduledContextInterceptor}.
 *
 * <p>Verifies the CDI interceptor context propagation for scheduled tasks including:
 * <ul>
 * <li>Generating a single UUID for both RequestId and TransactionId</li>
 * <li>Setting ThreadLocal context via RequestId and TransactionId</li>
 * <li>Tagging MDC via KibanaLogFields with req_id and tx_id</li>
 * <li>Cleaning up ThreadLocal and MDC in a finally block</li>
 * <li>Propagating return values and exceptions from the intercepted method</li>
 * </ul>
 */
@DisplayName("Quarkus Core - Scheduled Context Interceptor")
public class ScheduledContextInterceptorTest extends UnitTest {

    private ScheduledContextInterceptor interceptor;

    @Override
    @BeforeEach
    public void setUp() {
        interceptor = new ScheduledContextInterceptor();
    }

    @AfterEach
    public void tearDown() {
        // Clean up ThreadLocals and MDC to prevent test pollution if a test fails mid-execution
        RequestId.remove();
        TransactionId.remove();
        KibanaLogFields.clear();
    }

    @Test
    @DisplayName("Should set RequestId and TransactionId with the same UUID before proceeding")
    public void shouldSetRequestIdAndTransactionIdWithSameUuidBeforeProceeding() throws Exception {
        // Given: An InvocationContext that captures ThreadLocal state during proceed()
        final InvocationContext context = mock(InvocationContext.class);
        final String[] capturedRequestId = new String[1];
        final String[] capturedTransactionId = new String[1];

        when(context.proceed()).thenAnswer(invocation -> {
            capturedRequestId[0] = RequestId.get();
            capturedTransactionId[0] = TransactionId.get();
            return null;
        });

        // When: The interceptor processes the method
        interceptor.aroundInvoke(context);

        // Then: Both IDs were set and share the same UUID value
        assertThat(capturedRequestId[0], is(notNullValue()));
        assertThat(capturedTransactionId[0], is(notNullValue()));
        assertThat(capturedRequestId[0], is(equalTo(capturedTransactionId[0])));
    }

    @Test
    @DisplayName("Should tag MDC with req_id and tx_id before proceeding")
    public void shouldTagMdcWithReqIdAndTxIdBeforeProceeding() throws Exception {
        // Given: An InvocationContext that captures MDC state during proceed()
        final InvocationContext context = mock(InvocationContext.class);
        final String[] capturedMdcReqId = new String[1];
        final String[] capturedMdcTxId = new String[1];

        when(context.proceed()).thenAnswer(invocation -> {
            capturedMdcReqId[0] = MDC.get("req_id");
            capturedMdcTxId[0] = MDC.get("tx_id");
            return null;
        });

        // When: The interceptor processes the method
        interceptor.aroundInvoke(context);

        // Then: MDC fields were set during execution
        assertThat(capturedMdcReqId[0], is(notNullValue()));
        assertThat(capturedMdcTxId[0], is(notNullValue()));
        assertThat(capturedMdcReqId[0], is(equalTo(capturedMdcTxId[0])));
    }

    @Test
    @DisplayName("Should clear RequestId, TransactionId, and MDC after successful execution")
    public void shouldClearRequestIdTransactionIdAndMdcAfterSuccessfulExecution() throws Exception {
        // Given: An InvocationContext that completes without exception
        final InvocationContext context = mock(InvocationContext.class);
        when(context.proceed()).thenReturn("ok");

        // When: The interceptor processes the method successfully
        interceptor.aroundInvoke(context);

        // Then: ThreadLocals and MDC are cleaned up
        assertThat(RequestId.get(), is(nullValue()));
        assertThat(TransactionId.get(), is(nullValue()));
        assertThat(MDC.get("req_id"), is(nullValue()));
        assertThat(MDC.get("tx_id"), is(nullValue()));
    }

    @Test
    @DisplayName("Should clear RequestId, TransactionId, and MDC even when method throws")
    public void shouldClearRequestIdTransactionIdAndMdcEvenWhenMethodThrows() throws Exception {
        // Given: An InvocationContext that throws a RuntimeException during proceed()
        final InvocationContext context = mock(InvocationContext.class);
        when(context.proceed()).thenThrow(new RuntimeException("scheduled task failed"));

        // When: The interceptor processes the method (exception is expected)
        try {
            interceptor.aroundInvoke(context);
        } catch (final RuntimeException ignored) {
            // expected — exception propagation is tested separately
        }

        // Then: ThreadLocals and MDC are still cleaned up (finally block)
        assertThat(RequestId.get(), is(nullValue()));
        assertThat(TransactionId.get(), is(nullValue()));
        assertThat(MDC.get("req_id"), is(nullValue()));
        assertThat(MDC.get("tx_id"), is(nullValue()));
    }

    @Test
    @DisplayName("Should propagate exception from intercepted method")
    public void shouldPropagateExceptionFromInterceptedMethod() throws Exception {
        // Given: An InvocationContext that throws during proceed()
        final InvocationContext context = mock(InvocationContext.class);
        final RuntimeException expectedException = new RuntimeException("scheduled task failed");
        when(context.proceed()).thenThrow(expectedException);

        // When & Then: Exception is re-thrown by the interceptor
        try {
            interceptor.aroundInvoke(context);
            throw new AssertionError("Expected exception was not thrown");
        } catch (final RuntimeException e) {
            assertThat(e.getMessage(), is("scheduled task failed"));
        }
    }

}
