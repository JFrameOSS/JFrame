package io.github.jframe.logging.scheduled;

import io.github.jframe.logging.ecs.EcsFields;
import io.github.jframe.logging.model.RequestId;
import io.github.jframe.logging.model.TransactionId;
import io.github.support.UnitTest;

import java.util.List;

import org.aspectj.lang.ProceedingJoinPoint;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.InOrder;
import org.mockito.Mock;

import static io.github.jframe.logging.ecs.EcsFieldNames.REQUEST_ID;
import static io.github.jframe.logging.ecs.EcsFieldNames.TX_ID;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.inOrder;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link ScheduledAspect}.
 *
 * <p>Verifies the strategy-pattern enricher lifecycle:
 * <ul>
 * <li>TX_ID and REQUEST_ID are set in MDC during execution and cleared after</li>
 * <li>Enrichers are called during execution and their AutoCloseables closed in reverse order</li>
 * <li>Cleanup always happens even on exceptions</li>
 * <li>Enricher failures do not break scheduled task execution</li>
 * </ul>
 */
@DisplayName("Scheduled - ScheduledAspect")
class ScheduledAspectTest extends UnitTest {

    @Mock
    private ProceedingJoinPoint joinPoint;

    @AfterEach
    public void tearDown() {
        RequestId.remove();
        TransactionId.remove();
        EcsFields.clear();
    }

    @Test
    @DisplayName("Should set REQUEST_ID and TX_ID in MDC during execution and clear after")
    void withTransactionId_shouldSetRequestIdAndTxId() throws Throwable {
        // Given: A ScheduledAspect with no enrichers
        final ScheduledAspect aspect = new ScheduledAspect(List.of());
        when(joinPoint.proceed()).thenAnswer(invocation -> {
            // During execution: REQUEST_ID and TX_ID must be populated in MDC
            assertThat(EcsFields.get(REQUEST_ID), is(notNullValue()));
            assertThat(EcsFields.get(TX_ID), is(notNullValue()));
            assertThat(RequestId.get(), is(notNullValue()));
            assertThat(TransactionId.get(), is(notNullValue()));
            // REQUEST_ID and TX_ID should be the same UUID
            assertThat(EcsFields.get(REQUEST_ID), is(EcsFields.get(TX_ID)));
            return "done";
        });

        // When: The aspect executes
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: MDC and ThreadLocals are cleared after execution
        assertThat(result, is("done"));
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is(nullValue()));
        assertThat(RequestId.get(), is(nullValue()));
        assertThat(TransactionId.get(), is(nullValue()));
    }

    @Test
    @DisplayName("Should call enricher and close AutoCloseable on success")
    void withTransactionId_shouldCallEnricherAndCloseOnSuccess() throws Throwable {
        // Given: A single enricher that returns an AutoCloseable
        final ScheduledTaskEnricher enricher = mock(ScheduledTaskEnricher.class);
        final AutoCloseable closeable = mock(AutoCloseable.class);
        when(enricher.enrich(joinPoint)).thenReturn(closeable);
        when(joinPoint.proceed()).thenReturn("result");

        final ScheduledAspect aspect = new ScheduledAspect(List.of(enricher));

        // When: The aspect executes
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: Enricher was called and its closeable was closed
        assertThat(result, is("result"));
        verify(enricher).enrich(joinPoint);
        verify(closeable).close();
    }

    @Test
    @DisplayName("Should close enrichers in reverse order")
    void withTransactionId_shouldCloseEnrichersInReverseOrder() throws Throwable {
        // Given: Two enrichers
        final ScheduledTaskEnricher enricher1 = mock(ScheduledTaskEnricher.class);
        final ScheduledTaskEnricher enricher2 = mock(ScheduledTaskEnricher.class);
        final AutoCloseable closeable1 = mock(AutoCloseable.class);
        final AutoCloseable closeable2 = mock(AutoCloseable.class);
        when(enricher1.enrich(joinPoint)).thenReturn(closeable1);
        when(enricher2.enrich(joinPoint)).thenReturn(closeable2);
        when(joinPoint.proceed()).thenReturn("ok");

        final ScheduledAspect aspect = new ScheduledAspect(List.of(enricher1, enricher2));

        // When: The aspect executes
        aspect.withTransactionId(joinPoint);

        // Then: Closeables are closed in reverse order (closeable2 before closeable1)
        final InOrder inOrder = inOrder(closeable1, closeable2);
        inOrder.verify(closeable2).close();
        inOrder.verify(closeable1).close();
    }

    @Test
    @DisplayName("Should still close enrichers and clear MDC when proceed throws")
    void withTransactionId_whenProceedThrows_shouldStillCloseEnrichersAndClearMdc() throws Throwable {
        // Given: An enricher and a failing proceed()
        final ScheduledTaskEnricher enricher = mock(ScheduledTaskEnricher.class);
        final AutoCloseable closeable = mock(AutoCloseable.class);
        when(enricher.enrich(joinPoint)).thenReturn(closeable);
        when(joinPoint.proceed()).thenThrow(new RuntimeException("task failed"));

        final ScheduledAspect aspect = new ScheduledAspect(List.of(enricher));

        // When: The aspect executes and throws
        assertThrows(RuntimeException.class, () -> aspect.withTransactionId(joinPoint));

        // Then: Closeable is still closed
        verify(closeable).close();

        // And: MDC and ThreadLocals are cleared
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is(nullValue()));
        assertThat(RequestId.get(), is(nullValue()));
        assertThat(TransactionId.get(), is(nullValue()));
    }

    @Test
    @DisplayName("Should not break execution when enricher.enrich() throws")
    void withTransactionId_whenEnricherThrows_shouldNotBreakExecution() throws Throwable {
        // Given: An enricher that throws during enrich()
        final ScheduledTaskEnricher failingEnricher = mock(ScheduledTaskEnricher.class);
        when(failingEnricher.enrich(joinPoint)).thenThrow(new RuntimeException("enricher broken"));
        when(joinPoint.proceed()).thenReturn("success");

        final ScheduledAspect aspect = new ScheduledAspect(List.of(failingEnricher));

        // When: The aspect executes
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: Proceed was still called and result returned
        assertThat(result, is("success"));
        verify(joinPoint).proceed();
    }

    @Test
    @DisplayName("Should work correctly with no enrichers")
    void withTransactionId_withNoEnrichers_shouldStillWork() throws Throwable {
        // Given: A ScheduledAspect with empty enricher list
        final ScheduledAspect aspect = new ScheduledAspect(List.of());
        when(joinPoint.proceed()).thenReturn("scheduled-result");

        // When: The aspect executes
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: Basic TX_ID/REQUEST_ID lifecycle still works
        assertThat(result, is("scheduled-result"));
        assertThat(EcsFields.get(REQUEST_ID), is(nullValue()));
        assertThat(EcsFields.get(TX_ID), is(nullValue()));
    }

    @Test
    @DisplayName("Should treat null return from enricher.enrich() as no-op")
    void withTransactionId_whenEnricherReturnsNull_shouldTreatAsNoOp() throws Throwable {
        // Given: An enricher that returns null instead of an AutoCloseable
        final ScheduledTaskEnricher enricher = mock(ScheduledTaskEnricher.class);
        when(enricher.enrich(joinPoint)).thenReturn(null);
        when(joinPoint.proceed()).thenReturn("ok");

        final ScheduledAspect aspect = new ScheduledAspect(List.of(enricher));

        // When: The aspect executes (should not throw NPE)
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: Execution completes normally
        assertThat(result, is("ok"));
    }

    @Test
    @DisplayName("Should close remaining enrichers when one close() fails")
    void withTransactionId_whenOneCloseableFails_shouldStillCloseRemaining() throws Throwable {
        // Given: Two enrichers, first closeable throws on close
        final ScheduledTaskEnricher enricher1 = mock(ScheduledTaskEnricher.class);
        final ScheduledTaskEnricher enricher2 = mock(ScheduledTaskEnricher.class);
        final AutoCloseable closeable1 = mock(AutoCloseable.class);
        final AutoCloseable closeable2 = mock(AutoCloseable.class);
        when(enricher1.enrich(joinPoint)).thenReturn(closeable1);
        when(enricher2.enrich(joinPoint)).thenReturn(closeable2);
        // closeable2 closes first (reverse order), and it throws
        doThrow(new RuntimeException("close failed")).when(closeable2).close();
        when(joinPoint.proceed()).thenReturn("ok");

        final ScheduledAspect aspect = new ScheduledAspect(List.of(enricher1, enricher2));

        // When: The aspect executes
        final Object result = aspect.withTransactionId(joinPoint);

        // Then: Both closeables had close() attempted
        assertThat(result, is("ok"));
        verify(closeable2).close();
        verify(closeable1).close();
    }
}
