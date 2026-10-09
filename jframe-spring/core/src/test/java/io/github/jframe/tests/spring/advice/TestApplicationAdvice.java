package io.github.jframe.tests.spring.advice;

import java.util.Map;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

/** Application advice with no explicit order; must win over jFrame's catch-all for its exception types. */
@RestControllerAdvice
public class TestApplicationAdvice {

    /** Handled-by marker returned in app bodies. */
    public static final String HANDLED_BY = "app-advice";

    /** Handles {@link AppDomainException}. */
    @ExceptionHandler(AppDomainException.class)
    public ResponseEntity<Map<String, String>> handleDomain(final AppDomainException exception) {
        return ResponseEntity.status(HttpStatus.PAYMENT_REQUIRED).body(Map.of("handledBy", HANDLED_BY));
    }

    /** Handles a jFrame {@code HttpException} subtype. */
    @ExceptionHandler(AppHandledHttpException.class)
    public ResponseEntity<Map<String, String>> handleHttp(final AppHandledHttpException exception) {
        return ResponseEntity.status(HttpStatus.ACCEPTED).body(Map.of("handledBy", HANDLED_BY));
    }
}
