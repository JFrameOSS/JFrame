package io.github.jframe.exception.resource;

import lombok.NoArgsConstructor;

import org.springframework.web.bind.MethodArgumentNotValidException;

/** Response resource for {@link MethodArgumentNotValidException} validation errors. */
@NoArgsConstructor
public class MethodArgumentNotValidResponseResource extends ValidationErrorResponseResource {

    /** Constructor with a {@code validationException}. */
    public MethodArgumentNotValidResponseResource(final MethodArgumentNotValidException validationException) {
        super(validationException);
    }
}
