package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.mapper.AbstractExceptionMapper;
import io.github.jframe.exception.mapper.ConstraintViolationExceptionMapper;
import io.github.jframe.exception.mapper.HttpExceptionMapper;
import io.github.jframe.exception.mapper.InvalidPageExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSearchExceptionMapper;
import io.github.jframe.exception.mapper.InvalidSortExceptionMapper;
import io.github.jframe.exception.mapper.RateLimitExceededExceptionMapper;
import io.github.jframe.exception.mapper.ThrowableMapper;
import io.github.jframe.exception.mapper.ValidationExceptionMapper;
import io.github.jframe.exception.mapper.WebApplicationExceptionMapper;
import io.github.support.ProblemJson;

import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.Response;
import jakarta.ws.rs.ext.ExceptionMapper;

import org.jboss.resteasy.spi.ResteasyProviderFactory;

import static io.github.jframe.exception.factory.ProblemDetailsFixture.wire;

/**
 * Maps exceptions through JAX-RS mapper selection (most specific mapper wins), as the runtime would.
 */
final class QuarkusProblems {

    private QuarkusProblems() {
    }

    /**
     * Maps the exception with the most specific jFrame mapper, wired with the builder and request.
     *
     * <p>Mappers are wired after registration because RESTEasy replaces {@code @Context} fields on registration.
     */
    @SuppressWarnings("unchecked")
    static Response map(final Throwable exception, final ErrorResponseEntityBuilder builder, final ContainerRequestContext request) {
        final List<AbstractExceptionMapper<?>> mappers = List.of(
            new HttpExceptionMapper(),
            new ValidationExceptionMapper(),
            new RateLimitExceededExceptionMapper(),
            new InvalidSortExceptionMapper(),
            new InvalidSearchExceptionMapper(),
            new InvalidPageExceptionMapper(),
            new WebApplicationExceptionMapper(),
            new ConstraintViolationExceptionMapper(),
            new ThrowableMapper()
        );
        final ResteasyProviderFactory factory = ResteasyProviderFactory.newInstance();
        mappers.forEach(factory::registerProviderInstance);
        mappers.forEach(mapper -> wire(mapper, builder, request));
        final ExceptionMapper<Throwable> mapper = (ExceptionMapper<Throwable>) factory.getExceptionMapper(exception.getClass());
        return mapper.toResponse(exception);
    }

    /** Serialises the entity and asserts RFC 9457 conformance. */
    static String conformantJson(final Response response) {
        final String json = ProblemJson.toJson(response.getEntity());
        ProblemJson.assertRfc9457(json, response.getStatus());
        return json;
    }
}
