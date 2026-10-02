package io.github.jframe.exception.factory;

import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.page.InvalidPageErrorResponseResource;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchErrorResponseResource;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortErrorResponseResource;
import io.github.jframe.exception.sort.InvalidSortException;

import java.util.List;
import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;

/**
 * Factory that creates the appropriate {@link ErrorResponseResource} subtype based on the exception type.
 *
 * <p>Traverses the cause chain to find a known JFrame exception type.
 * Known types: {@link InvalidSortException}, {@link InvalidSearchException}, {@link InvalidPageException},
 * {@link ValidationException}, {@link RateLimitExceededException},
 * {@link jakarta.validation.ConstraintViolationException}.
 */
@ApplicationScoped
public class DefaultErrorResponseFactory implements ExceptionResponseFactory {

    private static final List<Class<? extends Throwable>> KNOWN_TYPES = List.of(
        InvalidSortException.class,
        InvalidSearchException.class,
        InvalidPageException.class,
        ValidationException.class,
        RateLimitExceededException.class,
        ConstraintViolationException.class
    );

    /**
     * Creates an {@link ErrorResponseResource} for the given throwable.
     *
     * <p>Traverses the cause chain to find a known exception type. If no known type is found,
     * returns a base {@link ErrorResponseResource}.
     *
     * @param throwable the throwable to create a resource for
     * @return the appropriate error response resource
     */
    @Override
    public ErrorResponseResource create(final Throwable throwable) {
        final Throwable resolved = resolve(throwable);
        return getErrorResponseResource(resolved, throwable);
    }

    private static ErrorResponseResource getErrorResponseResource(final Throwable resolved, final Throwable original) {
        return switch (resolved) {
            case final InvalidSortException e -> new InvalidSortErrorResponseResource(e);
            case final InvalidSearchException e -> new InvalidSearchErrorResponseResource(e);
            case final InvalidPageException e -> new InvalidPageErrorResponseResource(e);
            case final ValidationException e -> new ValidationErrorResponseResource(e);
            case final RateLimitExceededException e -> new RateLimitErrorResponseResource(e);
            case final ConstraintViolationException e -> new ConstraintViolationResponseResource(e);
            case null, default -> new ErrorResponseResource(original);
        };
    }

    private static boolean isKnown(final Throwable throwable) {
        return KNOWN_TYPES.stream().anyMatch(type -> type.isInstance(throwable));
    }

    private static Throwable resolve(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (isKnown(current)) {
                return current;
            }
            current = current.getCause();
        }
        return null;
    }
}
