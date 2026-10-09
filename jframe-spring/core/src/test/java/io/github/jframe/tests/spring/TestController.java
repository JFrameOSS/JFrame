package io.github.jframe.tests.spring;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ResourceNotFoundException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.jframe.validation.ValidationResult;
import io.github.support.fixtures.TestApiError;

import java.time.OffsetDateTime;
import java.util.List;
import java.util.Map;
import jakarta.ws.rs.core.Response;

import org.springframework.core.MethodParameter;
import org.springframework.security.access.AccessDeniedException;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.validation.BeanPropertyBindingResult;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

/**
 * REST controller exposing endpoints that throw exceptions for the error-handling contract tests.
 */
@RestController
@RequestMapping("/test")
public class TestController {

    /** Reset date used by the rate-limit endpoint. */
    public static final OffsetDateTime RESET_DATE = OffsetDateTime.parse("2030-01-01T12:00:00Z");

    /** Message of the internal cause that must never leak. */
    public static final String SECRET_MESSAGE = "jdbc:postgresql://db/secret password=hunter2";

    /** Throws {@link BadRequestException} — expected HTTP 400. */
    @GetMapping("/bad-request")
    public void badRequest() {
        throw new BadRequestException();
    }

    /** Throws {@link ResourceNotFoundException} — expected HTTP 404. */
    @GetMapping("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException();
    }

    /** Throws a business {@link HttpException} — expected HTTP 409. */
    @GetMapping("/business")
    public void business() {
        throw new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));
    }

    /** Throws an {@link HttpException} wrapping an internal cause — expected HTTP 409. */
    @GetMapping("/business-with-cause")
    public void businessWithCause() {
        throw new HttpException(
            new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT),
            new IllegalStateException(SECRET_MESSAGE)
        );
    }

    /** Throws an {@link HttpException} whose error code is not URI-safe — expected HTTP 400. */
    @GetMapping("/unsafe-code")
    public void unsafeCode() {
        throw new HttpException(new TestApiError("BAD CODE/1", "Unsafe code", Response.Status.BAD_REQUEST));
    }

    /** Throws {@link RateLimitExceededException} — expected HTTP 429. */
    @GetMapping("/rate-limit")
    public void rateLimit() {
        throw new RateLimitExceededException(100, 0, RESET_DATE);
    }

    /** Throws {@link ValidationException} with two field rejections — expected HTTP 400. */
    @GetMapping("/validation-error")
    public void validationError() {
        final ValidationResult result = new ValidationResult();
        result.rejectValue("name", "name.required");
        result.rejectValue("email", "email.required");
        throw new ValidationException(result);
    }

    /** Throws {@link MethodArgumentNotValidException} as bean validation would — expected HTTP 400. */
    @GetMapping("/bean-validation")
    public void beanValidation() throws Exception {
        final BeanPropertyBindingResult bindingResult = new BeanPropertyBindingResult(new Object(), "request");
        bindingResult.addError(
            new FieldError(
                "request",
                "name",
                null,
                false,
                new String[] {
                    "NotBlank"
                },
                null,
                "must not be blank"
            )
        );
        final MethodParameter parameter = new MethodParameter(TestController.class.getMethod("beanValidation"), -1);
        throw new MethodArgumentNotValidException(parameter, bindingResult);
    }

    /** Throws {@link InvalidSortException} — expected HTTP 400. */
    @GetMapping("/invalid-sort")
    public void invalidSort() {
        throw new InvalidSortException("password", List.of("name", "createdAt"));
    }

    /** Throws {@link InvalidSearchException} — expected HTTP 400. */
    @GetMapping("/invalid-search")
    public void invalidSearch() {
        throw new InvalidSearchException("age", "abc", List.of("name", "age"));
    }

    /** Throws {@link InvalidPageException} — expected HTTP 400. */
    @GetMapping("/invalid-page")
    public void invalidPage() {
        throw new InvalidPageException("size", -1);
    }

    /** Throws {@link BadCredentialsException} — expected HTTP 401. */
    @GetMapping("/bad-credentials")
    public void badCredentials() {
        throw new BadCredentialsException(SECRET_MESSAGE);
    }

    /** Throws {@link AccessDeniedException} — expected HTTP 403. */
    @GetMapping("/access-denied")
    public void accessDenied() {
        throw new AccessDeniedException(SECRET_MESSAGE);
    }

    /** Throws an unexpected exception — expected HTTP 500. */
    @GetMapping("/unexpected")
    public void unexpected() {
        throw new IllegalStateException(SECRET_MESSAGE);
    }

    /** Accepts JSON only; used for 415 and unreadable-body scenarios. */
    @PostMapping(
        path = "/json-body",
        consumes = "application/json"
    )
    public Map<String, Object> jsonBody(@RequestBody final Map<String, Object> body) {
        return body;
    }
}
