package io.github.jframe.exception.factory;

import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ConstraintViolationResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;

import jakarta.validation.ConstraintViolationException;

/**
 * Factory that creates the appropriate {@link ErrorResponseResource} subtype based on the exception type.
 *
 * <p>Traverses the cause chain to find a known JFrame exception type.
 */
public class DefaultErrorResponseFactory {

    /**
     * Creates an {@link ErrorResponseResource} for the given throwable.
     *
     * <p>Traverses the cause chain to find a known exception type. If no known type is found,
     * returns a base {@link ErrorResponseResource}.
     *
     * @param throwable the throwable to create a resource for
     * @return the appropriate error response resource
     */
    public ErrorResponseResource create(final Throwable throwable) {
        final Throwable resolved = resolve(throwable);
        final ErrorResponseResource resource;

        if (resolved instanceof ApiException) {
            resource = new ApiErrorResponseResource((ApiException) resolved);
        } else if (resolved instanceof ValidationException) {
            resource = new ValidationErrorResponseResource((ValidationException) resolved);
        } else if (resolved instanceof RateLimitExceededException) {
            resource = new RateLimitErrorResponseResource(resolved);
        } else if (resolved instanceof ConstraintViolationException) {
            resource = new ConstraintViolationResponseResource(resolved);
        } else {
            resource = new ErrorResponseResource(throwable);
        }

        return resource;
    }

    private Throwable resolve(final Throwable throwable) {
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
