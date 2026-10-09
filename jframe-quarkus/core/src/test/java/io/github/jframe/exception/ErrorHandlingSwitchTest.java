package io.github.jframe.exception;

import io.github.jframe.exception.enricher.ConstraintViolationResponseEnricher;
import io.github.jframe.exception.enricher.ErrorCodeResponseEnricher;
import io.github.jframe.exception.enricher.RateLimitResponseEnricher;
import io.github.jframe.exception.enricher.TransactionIdResponseEnricher;
import io.github.jframe.exception.enricher.ValidationErrorResponseEnricher;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.mapper.HttpExceptionMapper;
import io.github.jframe.exception.mapper.InvalidPageExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSearchExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSortExceptionMapper;
import io.github.jframe.exception.mapper.RateLimitExceededExceptionMapper;
import io.github.jframe.exception.mapper.ThrowableMapper;
import io.github.jframe.exception.mapper.ValidationExceptionMapper;
import io.github.support.UnitTest;
import io.quarkus.arc.properties.IfBuildProperty;

import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Error-handling beans must be switchable via {@code jframe.exception.enabled} (default on).
 */
@DisplayName("Unit Test - Quarkus error handling switch")
class ErrorHandlingSwitchTest extends UnitTest {

    private static final List<Class<?>> ERROR_HANDLING_BEANS = List.of(
        HttpExceptionMapper.class,
        RateLimitExceededExceptionMapper.class,
        ValidationExceptionMapper.class,
        InvalidSortExceptionMapper.class,
        InvalidSearchExceptionMapper.class,
        InvalidPageExceptionMapper.class,
        ThrowableMapper.class,
        ErrorResponseEntityBuilder.class,
        ErrorCodeResponseEnricher.class,
        TransactionIdResponseEnricher.class,
        RateLimitResponseEnricher.class,
        ValidationErrorResponseEnricher.class,
        ConstraintViolationResponseEnricher.class
    );

    @Test
    @DisplayName("Should guard every error-handling bean with jframe.exception.enabled, enabled when missing")
    void shouldGuardErrorHandlingBeansWithEnabledProperty() {
        // Given: all jFrame error-handling beans
        for (final Class<?> bean : ERROR_HANDLING_BEANS) {
            // When: reading the build-property condition
            final IfBuildProperty condition = bean.getAnnotation(IfBuildProperty.class);

            // Then: bean is active only if jframe.exception.enabled=true (default when missing)
            assertThat(bean.getSimpleName(), condition, is(notNullValue()));
            assertThat(bean.getSimpleName(), condition.name(), is("jframe.exception.enabled"));
            assertThat(bean.getSimpleName(), condition.stringValue(), is("true"));
            assertThat(bean.getSimpleName(), condition.enableIfMissing(), is(true));
        }
    }
}
