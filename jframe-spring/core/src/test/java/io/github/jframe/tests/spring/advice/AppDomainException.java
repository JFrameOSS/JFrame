package io.github.jframe.tests.spring.advice;

/** Application-owned exception handled by {@link TestApplicationAdvice}. */
public class AppDomainException extends RuntimeException {

    private static final long serialVersionUID = 1L;

    /** Creates the exception. */
    public AppDomainException(final String message) {
        super(message);
    }
}
