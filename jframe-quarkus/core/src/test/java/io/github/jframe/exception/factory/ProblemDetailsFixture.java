package io.github.jframe.exception.factory;

import io.github.jframe.exception.assembler.ConstraintViolationResourceAssembler;
import io.github.jframe.exception.assembler.ValidationErrorResourceAssembler;
import io.github.jframe.exception.enricher.ConstraintViolationResponseEnricher;
import io.github.jframe.exception.enricher.ErrorCodeResponseEnricher;
import io.github.jframe.exception.enricher.ErrorResponseEnricher;
import io.github.jframe.exception.enricher.RateLimitResponseEnricher;
import io.github.jframe.exception.enricher.TransactionIdResponseEnricher;
import io.github.jframe.exception.enricher.ValidationErrorResponseEnricher;
import io.github.jframe.exception.mapper.AbstractExceptionMapper;
import tools.jackson.databind.json.JsonMapper;

import java.lang.reflect.Field;
import java.net.URI;
import java.util.ArrayList;
import java.util.List;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.UriInfo;

import org.mockito.quality.Strictness;

import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.withSettings;

/**
 * Wires Quarkus exception mappers with jFrame's built-in enrichers, as CDI would.
 *
 * <p>Contract: Problem Details standard members ({@code type}, {@code title}, {@code status},
 * {@code detail}, {@code instance}) are populated by the builder/mapper, not by optional enrichers.
 */
public final class ProblemDetailsFixture {

    private ProblemDetailsFixture() {
    }

    /** jFrame's built-in enrichers plus the given application enrichers (appended last). */
    public static List<ErrorResponseEnricher> builtInEnrichersPlus(final ErrorResponseEnricher... extra) {
        final JsonMapper mapper = JsonMapper.builder().build();
        final List<ErrorResponseEnricher> enrichers = new ArrayList<>(
            List.of(
                new ErrorCodeResponseEnricher(),
                new TransactionIdResponseEnricher(),
                new RateLimitResponseEnricher(),
                new ValidationErrorResponseEnricher(new ValidationErrorResourceAssembler(mapper)),
                new ConstraintViolationResponseEnricher(new ConstraintViolationResourceAssembler(mapper))
            )
        );
        enrichers.addAll(List.of(extra));
        return enrichers;
    }

    /** A builder with the given enrichers. */
    public static ErrorResponseEntityBuilder aBuilder(final List<ErrorResponseEnricher> enrichers) {
        return new ErrorResponseEntityBuilder(new DefaultErrorResponseFactory(), enrichers);
    }

    /** A GET request context for the given path. */
    public static ContainerRequestContext aRequest(final String path) {
        final ContainerRequestContext context = mock(ContainerRequestContext.class, withSettings().strictness(Strictness.LENIENT));
        final UriInfo uriInfo = mock(UriInfo.class, withSettings().strictness(Strictness.LENIENT));
        when(uriInfo.getRequestUri()).thenReturn(URI.create("http://localhost:8080" + path + "?q=secret"));
        when(uriInfo.getPath()).thenReturn(path);
        when(context.getUriInfo()).thenReturn(uriInfo);
        when(context.getMethod()).thenReturn("GET");
        return context;
    }

    /** Injects builder and request context into the mapper, as CDI/JAX-RS would. */
    public static <M extends AbstractExceptionMapper<?>> M wire(final M mapper, final ErrorResponseEntityBuilder builder,
        final ContainerRequestContext requestContext) {
        set(mapper, ErrorResponseEntityBuilder.class, builder);
        set(mapper, ContainerRequestContext.class, requestContext);
        return mapper;
    }

    private static void set(final Object target, final Class<?> type, final Object value) {
        for (final Field field : AbstractExceptionMapper.class.getDeclaredFields()) {
            if (field.getType().equals(type)) {
                try {
                    field.setAccessible(true);
                    field.set(target, value);
                    return;
                } catch (final IllegalAccessException exception) {
                    throw new IllegalStateException(exception);
                }
            }
        }
        throw new IllegalStateException("No field of type " + type.getName() + " in AbstractExceptionMapper");
    }
}
