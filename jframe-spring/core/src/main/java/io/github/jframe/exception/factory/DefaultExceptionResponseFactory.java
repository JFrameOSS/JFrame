package io.github.jframe.exception.factory;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.RateLimitErrorResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

import static java.util.Objects.nonNull;

/**
 * Default type of {@link ExceptionResponseFactory}.
 *
 * <p>The default type creates a {@link ErrorResponseResource} for known exception types, and falls
 * back to a plain {@link ErrorResponseResource} for all other cases.
 */
@Component
public class DefaultExceptionResponseFactory implements ExceptionResponseFactory {

    /**
     * Create the response resource.
     *
     * <p>If present, the first {@link JFrameException} found in the cause chain of the throwable is
     * used to determine the response factory. If there is no cause, or if it doesn't contain a {@link JFrameException}, the throwable
     * itself is used.
     *
     * <p>As an example, assume throwable is some type of {@link HttpException}, caused by a
     * {@link ValidationException}. In such a case, we want the error information to be derived from
     * the {@link ValidationException}.
     *
     * @param throwable the throwable
     * @return the error resource
     */
    @Override
    public ErrorResponseResource create(final Throwable throwable) {
        ErrorResponseResource result = null;
        if (nonNull(throwable)) {
            result = getErrorResponseResource(throwable);
            if (result == null) {
                final Throwable cause = getCausingJFrameException(throwable);
                result = getErrorResponseResource(cause);
            }
        }
        if (result == null) {
            result = new ErrorResponseResource(throwable);
        }
        return result;
    }

    /**
     * Create an instance of the correct type of error resource for the given throwable.
     *
     * @param throwable the throwable
     * @return the error resource
     */
    private static ErrorResponseResource getErrorResponseResource(final Throwable throwable) {
        return switch (throwable) {
            case final MethodArgumentNotValidException methodArgumentNotValidException -> new MethodArgumentNotValidResponseResource(
                methodArgumentNotValidException
            );
            case final ValidationException validationException -> new ValidationErrorResponseResource(validationException);
            case final RateLimitExceededException rateLimitExceededException -> new RateLimitErrorResponseResource(
                rateLimitExceededException
            );
            case null, default -> null;
        };
    }

    /**
     * Returns the first {@link JFrameException} in the cause chain, or the original throwable if none found.
     *
     * @param throwable the Throwable to examine, must not be {@code null}
     * @return a JFrameException, or throwable
     */
    private static Throwable getCausingJFrameException(final Throwable throwable) {
        Throwable current = throwable.getCause();
        while (current != null) {
            if (current instanceof JFrameException) {
                return current;
            }
            current = current.getCause();
        }
        return throwable;
    }
}
