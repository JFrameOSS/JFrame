package io.github.jframe.exception.factory;


import io.github.jframe.exception.resource.ErrorResponseResource;

/**
 * Factory to create the right factory of response resource for an exception.
 */
@FunctionalInterface
public interface ExceptionResponseFactory {

    /**
     * Create the response resource.
     *
     * @param throwable the exception
     * @return the error resource
     */
    ErrorResponseResource create(Throwable throwable);
}
