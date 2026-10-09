package io.github.jframe.exception.handler;

import org.springframework.web.bind.annotation.RestControllerAdvice;

/**
 * Marks {@link JFrameResponseEntityExceptionHandler} as controller advice without making it a scannable component.
 *
 * <p>Lets {@code ExceptionConfiguration} register the handler conditionally, even when apps scan {@code io.github.jframe}.
 */
@RestControllerAdvice
interface JFrameControllerAdvice {
}
