package io.github.jframe.exception.factory;


import io.github.jframe.exception.ApiException;
import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.JFrameException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.resource.ApiErrorResponseResource;
import io.github.jframe.exception.resource.ErrorResponseResource;
import io.github.jframe.exception.resource.MethodArgumentNotValidResponseResource;
import io.github.jframe.exception.resource.ValidationErrorResponseResource;

import org.springframework.stereotype.Component;
import org.springframework.web.bind.MethodArgumentNotValidException;

/**
 * Default type of {@link ExceptionResponseFactory}.
 *
 * <p>The default type creates an {@link ApiErrorResponseResource} if the exception is an
 * {@link ApiException} and a {@link ErrorResponseResource} in all other cases.
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
     * <p>As an example, assume throwable is some factory of {@link HttpException}, caused by an {@link
     * ApiException}. In such a case, we want the error information to be derived from the {@link ApiException}.
     *
     * @param throwable the throwable
     * @return the error resource
     */
    @Override
    public ErrorResponseResource create(final Throwable throwable) {
        ErrorResponseResource result = null;
        if (throwable != null) {
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
     * Create an instance of the correct factory of error resource for the given throwable.
     *
     * @param throwable the throwable
     * @return the error resource
     */
    private static ErrorResponseResource getErrorResponseResource(final Throwable throwable) {
        final ErrorResponseResource result;
        if (throwable instanceof final ApiException apiException) {
            result = new ApiErrorResponseResource(apiException);
        } else if (throwable instanceof final MethodArgumentNotValidException methodArgumentNotValidException) {
            result = new MethodArgumentNotValidResponseResource(methodArgumentNotValidException);
        } else if (throwable instanceof final ValidationException validationException) {
            result = new ValidationErrorResponseResource(validationException);
        } else {
            result = null;
        }
        return result;
    }

    /**
     * Returns the first {@link JFrameException} encountered in the chain of exception causes, or the original throwable if no
     * {@link JFrameException} can be found.
     *
     * @param throwable the Throwable to examine, must not be <code>null</code>
     * @return a JFrameException, or throwable
     */
    private static Throwable getCausingJFrameException(final Throwable throwable) {
        Throwable cause = getCause(throwable.getCause());
        if (cause == null) {
            cause = throwable;
        }
        return cause;
    }

    /**
     * Recursive method to find the cause of a Throwable, if that is a {@link JFrameException}.
     *
     * @param throwable the throwable
     * @return throwable, or null
     */
    private static Throwable getCause(final Throwable throwable) {
        final Throwable cause;
        if (throwable == null) {
            cause = null;
        } else if (throwable instanceof JFrameException) {
            cause = throwable;
        } else {
            cause = getCause(throwable.getCause());
        }
        return cause;
    }
}
