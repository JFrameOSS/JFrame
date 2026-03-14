package io.github.jframe.tests.spring;

import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.DataNotFoundException;
import io.github.jframe.exception.core.InternalServerErrorException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ResourceNotFoundException;
import io.github.jframe.exception.core.UnauthorizedRequestException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.validation.ValidationResult;

import java.time.OffsetDateTime;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller that exposes endpoints that throw JFrame exceptions for integration testing.
 *
 * <p>Each endpoint corresponds to a specific exception type so that
 * {@link ExceptionHandlingContractTest} and {@link ValidationContractTest} can trigger
 * and verify the correct HTTP response.
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /** Throws {@link BadRequestException} — expected HTTP 400. */
    @GetMapping("/bad-request")
    public void badRequest() {
        throw new BadRequestException("Bad request test");
    }

    /** Throws {@link ResourceNotFoundException} — expected HTTP 404. */
    @GetMapping("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException("Resource not found test");
    }

    /** Throws {@link DataNotFoundException} — expected HTTP 404. */
    @GetMapping("/data-not-found")
    public void dataNotFound() {
        throw new DataNotFoundException("Data not found test");
    }

    /** Throws {@link UnauthorizedRequestException} — expected HTTP 401. */
    @GetMapping("/unauthorized")
    public void unauthorized() {
        throw new UnauthorizedRequestException("Unauthorized test");
    }

    /** Throws {@link InternalServerErrorException} — expected HTTP 500. */
    @GetMapping("/internal-error")
    public void internalError() {
        throw new InternalServerErrorException("Internal error test");
    }

    /**
     * Throws {@link RateLimitExceededException} — expected HTTP 429.
     *
     * <p>Uses a fixed window with limit=100, remaining=0 and a reset date in the future.
     */
    @GetMapping("/rate-limit")
    public void rateLimit() {
        throw new RateLimitExceededException(100, 0, OffsetDateTime.now().plusHours(1));
    }

    /**
     * Throws {@link ValidationException} with two field rejections — expected HTTP 400
     * with a body containing the validation errors.
     */
    @GetMapping("/validation-error")
    public void validationError() {
        final ValidationResult result = new ValidationResult();
        result.rejectValue("name", "name.required");
        result.rejectValue("email", "email.required");
        throw new ValidationException(result);
    }
}
