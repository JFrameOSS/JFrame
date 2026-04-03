package io.github.jframe.exception.factory;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;

import jakarta.enterprise.context.ApplicationScoped;
import jakarta.validation.ConstraintViolationException;

/**
 * Factory that creates the appropriate {@link ErrorResponseResource} subtype based on the exception type.
 *
 * <p>Traverses the cause chain to find a known JFrame exception type.
 */
@ApplicationScoped
public class DefaultErrorResponseFactory implements ExceptionResponseFactory {

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
            case final ApiException e -> new ApiErrorResponseResource(e);
            case final ValidationException e -> new ValidationErrorResponseResource(e);
            case final RateLimitExceededException e -> new RateLimitErrorResponseResource(e);
            case final ConstraintViolationException e -> new ConstraintViolationResponseResource(e);
            case null, default -> new ErrorResponseResource(original);
        };
    }

    private static Throwable resolve(final Throwable throwable) {
        Throwable current = throwable;
        while (current != null) {
            if (current instanceof ApiException
                || current instanceof ValidationException
                || current instanceof RateLimitExceededException
                || current instanceof ConstraintViolationException) {
                return current;
            }
            current = current.getCause();
        }
        return throwable;
    }
}
