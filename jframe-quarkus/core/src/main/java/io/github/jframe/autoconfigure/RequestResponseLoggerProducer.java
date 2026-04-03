package io.github.jframe.autoconfigure;

import io.github.jframe.logging.LoggingConfig;
import io.github.jframe.logging.logger.DefaultRequestResponseLogger;
import io.github.jframe.logging.logger.HttpRequestResponseBodyLogger;
import io.github.jframe.logging.logger.HttpRequestResponseDebugLogger;
import io.github.jframe.logging.logger.HttpRequestResponseHeadersLogger;
import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.voter.MediaTypeVoter;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.enterprise.inject.Produces;

/**
 * CDI producer that creates a {@link RequestResponseLogger} bean configured with the
 * allowed and excluded content types from {@link LoggingConfig} and a {@link PasswordMasker}.
 *
 * <p>Quarkus equivalent of Spring's {@code CoreAutoConfiguration.requestResponseLogger()}.
 */
@Slf4j
@ApplicationScoped
@RequiredArgsConstructor
public class RequestResponseLoggerProducer {

    private final LoggingConfig loggingConfig;

    private final PasswordMasker passwordMasker;

    /**
     * Produces a {@link RequestResponseLogger} configured with headers/body listener,
     * a debug logger, and media type voters derived from {@link LoggingConfig}.
     *
     * @return the configured {@link RequestResponseLogger}
     */
    @Produces
    @ApplicationScoped
    public RequestResponseLogger requestResponseLogger() {
        final HttpRequestResponseHeadersLogger headersLogger =
            new HttpRequestResponseHeadersLogger(passwordMasker);
        final HttpRequestResponseBodyLogger bodyLogger =
            new HttpRequestResponseBodyLogger(passwordMasker);
        final HttpRequestResponseDebugLogger debugLogger = new HttpRequestResponseDebugLogger();

        final MediaTypeVoter mediaTypeVoter =
            new MediaTypeVoter(loggingConfig.allowedContentTypes(), true);
        final MediaTypeVoter bodyExcludedMediaTypeVoter =
            new MediaTypeVoter(loggingConfig.bodyExcludedContentTypes(), false);

        return new DefaultRequestResponseLogger(
            headersLogger,
            bodyLogger,
            debugLogger,
            mediaTypeVoter,
            bodyExcludedMediaTypeVoter
        );
    }
}
