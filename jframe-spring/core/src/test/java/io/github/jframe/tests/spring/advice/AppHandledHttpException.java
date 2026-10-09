package io.github.jframe.tests.spring.advice;

import io.github.jframe.exception.HttpException;
import io.github.support.fixtures.TestApiError;

import jakarta.ws.rs.core.Response;

/** jFrame {@link HttpException} subtype the application handles itself via {@link TestApplicationAdvice}. */
public class AppHandledHttpException extends HttpException {

    private static final long serialVersionUID = 1L;

    /** Creates the exception. */
    public AppHandledHttpException() {
        super(new TestApiError("APP_OWNED", "Handled by app", Response.Status.CONFLICT));
    }
}
