package io.github.jframe.tests.spring;

import io.github.jframe.exception.handler.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.support.Extensions;
import io.github.support.fixtures.TestApiError;

import jakarta.ws.rs.core.Response;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.core.Ordered;
import org.springframework.core.annotation.Order;
import org.springframework.http.HttpStatus;
import org.springframework.web.context.request.WebRequest;

/**
 * Application-defined enrichers used to verify custom enrichment and ordering.
 */
@TestConfiguration
public class TestEnricherConfiguration {

    /** Path whose error code and detail are overridden by the application. */
    public static final String OVERRIDE_PATH = "/test/not-found";

    /** Unordered enricher: runs after jFrame's built-in enrichers and after {@link #earlyEnricher()}. */
    @Bean
    public ErrorResponseEnricher lateEnricher() {
        return new ErrorResponseEnricher() {

            @Override
            public void doEnrich(final ErrorResponseResource resource, final Throwable throwable,
                final WebRequest request, final HttpStatus httpStatus) {
                Extensions.add(resource, "tenant", "acme");
                Extensions.add(resource, "enricherOrder", "late");
                if (request.getDescription(false).endsWith(OVERRIDE_PATH)) {
                    resource.setError(new TestApiError("ORDER_NOT_FOUND", "Order does not exist", Response.Status.NOT_FOUND));
                }
            }
        };
    }

    /** Explicitly ordered enricher that must run before {@link #lateEnricher()}. */
    @Bean
    @Order(Ordered.LOWEST_PRECEDENCE - 10)
    public ErrorResponseEnricher earlyEnricher() {
        return (resource, throwable, request, httpStatus) -> Extensions.add(resource, "enricherOrder", "early");
    }
}
