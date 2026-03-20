package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.logger.DefaultRequestResponseLogger;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.support.UnitTest;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.mockito.Mockito.when;

/**
 * Unit tests for {@link RequestResponseLoggerProducer}.
 *
 * <p>Verifies that the CDI producer creates a {@link RequestResponseLogger} configured with
 * the dependencies from {@link LoggingConfig} and {@link PasswordMasker}.
 */
@DisplayName("Autoconfigure - RequestResponseLoggerProducer")
public class RequestResponseLoggerProducerTest extends UnitTest {

    @Mock
    private LoggingConfig loggingConfig;

    @Mock
    private PasswordMasker passwordMasker;

    @Test
    @DisplayName("Should produce a non-null RequestResponseLogger when dependencies are provided")
    public void shouldProduceNonNullRequestResponseLoggerWhenDependenciesProvided() {
        // Given: A LoggingConfig with allowed and excluded content types
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.bodyExcludedContentTypes()).thenReturn(List.of("multipart/form-data"));
        final RequestResponseLoggerProducer producer = new RequestResponseLoggerProducer(loggingConfig, passwordMasker);

        // When: Producing the RequestResponseLogger
        final RequestResponseLogger logger = producer.requestResponseLogger();

        // Then: A non-null RequestResponseLogger is produced
        assertThat(logger, is(notNullValue()));
    }

    @Test
    @DisplayName("Should produce a DefaultRequestResponseLogger implementation")
    public void shouldProduceDefaultRequestResponseLoggerImplementation() {
        // Given: A LoggingConfig with allowed and excluded content types
        when(loggingConfig.allowedContentTypes()).thenReturn(List.of("application/json"));
        when(loggingConfig.bodyExcludedContentTypes()).thenReturn(List.of("multipart/form-data"));
        final RequestResponseLoggerProducer producer = new RequestResponseLoggerProducer(loggingConfig, passwordMasker);

        // When: Producing the RequestResponseLogger
        final RequestResponseLogger logger = producer.requestResponseLogger();

        // Then: The produced instance is of type DefaultRequestResponseLogger
        assertThat(logger, is(instanceOf(DefaultRequestResponseLogger.class)));
    }
}
