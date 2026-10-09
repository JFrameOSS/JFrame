package io.github.jframe.exception;

import io.github.jframe.exception.factory.DefaultExceptionResponseFactory;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.handler.JFrameResponseEntityExceptionHandler;
import io.github.jframe.exception.handler.enricher.ErrorCodeResponseEnricher;
import io.github.jframe.exception.handler.enricher.MethodArgumentNotValidResponseEnricher;
import io.github.jframe.exception.handler.enricher.RateLimitResponseEnricher;
import io.github.jframe.exception.handler.enricher.TransactionIdResponseEnricher;
import io.github.jframe.exception.handler.enricher.ValidationErrorResponseEnricher;
import io.github.jframe.exception.resource.ObjectErrorResourceAssembler;
import io.github.jframe.exception.resource.ValidationErrorResourceAssembler;

import org.springframework.boot.autoconfigure.condition.ConditionalOnBooleanProperty;
import org.springframework.boot.autoconfigure.condition.ConditionalOnMissingBean;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.Import;

/**
 * Aggregator configuration that registers all jframe exception-handling beans.
 *
 * <p>Imported by {@code CoreAutoConfiguration} to keep that class's import count within
 * PMD {@code ExcessiveImports} limits. {@code TransactionIdResponseEnricher} carries a
 * class-level {@code @ConditionalOnProperty} — {@code @Import} preserves it.
 */
@Configuration
@ConditionalOnBooleanProperty(
    name = ExceptionConfiguration.ENABLED_PROPERTY,
    matchIfMissing = true
)
@Import(
    {
        RateLimitResponseEnricher.class,
        TransactionIdResponseEnricher.class,
        ErrorCodeResponseEnricher.class,
        MethodArgumentNotValidResponseEnricher.class,
        ValidationErrorResponseEnricher.class,
        ObjectErrorResourceAssembler.class,
        ValidationErrorResourceAssembler.class,
        ErrorResponseEntityBuilder.class,
        DefaultExceptionResponseFactory.class
    }
)
public class ExceptionConfiguration {

    /** Switch for jFrame error handling (handler, builder, enrichers, OpenAPI error docs). */
    public static final String ENABLED_PROPERTY = "jframe.exception.enabled";

    /**
     * The global exception handler; backs off when the application defines its own.
     *
     * @param errorResponseEntityBuilder the builder
     * @return the handler
     */
    @Bean
    @ConditionalOnMissingBean(JFrameResponseEntityExceptionHandler.class)
    public JFrameResponseEntityExceptionHandler jFrameResponseEntityExceptionHandler(
        final ErrorResponseEntityBuilder errorResponseEntityBuilder) {
        return new JFrameResponseEntityExceptionHandler(errorResponseEntityBuilder);
    }
}
