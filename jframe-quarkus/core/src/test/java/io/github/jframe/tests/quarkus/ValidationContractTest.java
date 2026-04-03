package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.mapper.ValidationExceptionMapper;
import io.github.jframe.validation.ValidationResult;

import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Quarkus integration tests that verify validation contract behavior.
 *
 * <p>Uses the direct-mapper-invocation approach: the {@link ValidationExceptionMapper} is
 * instantiated directly, its {@code toResponse()} method is called with a
 * {@link ValidationException} carrying field errors, and the resulting {@link Response} is
 * verified to have HTTP 400 status and a non-null body containing the validation result.
 *
 * <p>This mirrors the behavior verified by {@code ValidationContractTest} in the
 * {@code jframe-tests-spring} module.
 */
@DisplayName("Quarkus Integration - Validation Contract Tests")
class ValidationContractTest {

    /** Mapper under test. */
    private final ValidationExceptionMapper mapper = new ValidationExceptionMapper();

    /**
     * Verifies that a {@link ValidationException} with field errors is mapped to HTTP 400
     * with a non-null response body containing the validation result.
     */
    @Test
    @DisplayName("Should return 400 with validation errors for invalid input")
    void shouldReturn400WithValidationErrors() {
        // Given: A ValidationException with two field rejections
        final ValidationResult result = new ValidationResult();
        result.rejectValue("name", "name.required");
        result.rejectValue("email", "email.required");
        final ValidationException exception = new ValidationException(result);

        // When: The mapper converts the exception to a response
        final Response response = mapper.toResponse(exception);

        // Then: Response has 400 status and contains the validation result
        assertThat(response.getStatus(), is(400));
        assertThat(response.getEntity(), is(notNullValue()));
    }
}
