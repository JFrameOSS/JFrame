package io.github.jframe.exception;

import io.github.jframe.exception.factory.DefaultExceptionResponseFactory;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.handler.JFrameResponseEntityExceptionHandler;
import io.github.jframe.exception.handler.enricher.ErrorCodeResponseEnricher;
import io.github.jframe.exception.handler.enricher.MethodArgumentNotValidResponseEnricher;
import io.github.jframe.exception.handler.enricher.RateLimitResponseEnricher;
import io.github.jframe.exception.handler.enricher.RequestInfoResponseEnricher;
import io.github.jframe.exception.handler.enricher.StatusCodeResponseEnricher;
import io.github.jframe.exception.handler.enricher.TransactionIdResponseEnricher;
import io.github.jframe.exception.handler.enricher.ValidationErrorResponseEnricher;
import io.github.jframe.exception.resource.ObjectErrorResourceAssembler;
import io.github.jframe.exception.resource.ValidationErrorResourceAssembler;

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
@Import(
    {
        RateLimitResponseEnricher.class,
        StatusCodeResponseEnricher.class,
        RequestInfoResponseEnricher.class,
        TransactionIdResponseEnricher.class,
        ErrorCodeResponseEnricher.class,
        MethodArgumentNotValidResponseEnricher.class,
        ValidationErrorResponseEnricher.class,
        JFrameResponseEntityExceptionHandler.class,
        ObjectErrorResourceAssembler.class,
        ValidationErrorResourceAssembler.class,
        ErrorResponseEntityBuilder.class,
        DefaultExceptionResponseFactory.class
    }
)
public class ExceptionConfiguration {}
