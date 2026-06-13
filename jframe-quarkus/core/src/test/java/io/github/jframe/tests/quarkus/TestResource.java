package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ResourceNotFoundException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.validation.ValidationResult;

import java.time.OffsetDateTime;
import jakarta.ws.rs.GET;
import jakarta.ws.rs.Path;
import jakarta.ws.rs.Produces;
import jakarta.ws.rs.core.MediaType;

/**
 * JAX-RS resource that exposes endpoints throwing JFrame exceptions for integration testing.
 *
 * <p>Each endpoint corresponds to a specific exception type so that contract tests can trigger
 * and verify the correct HTTP response via the Quarkus exception mappers defined in
 * {@code jframe-quarkus-core}.
 *
 * <p>Note: in the direct-mapper-invocation test approach (used by
 * {@link ExceptionHandlingContractTest} and {@link ValidationContractTest}) this resource is
 * present for completeness and future full-server tests. It is the JAX-RS equivalent of the
 * Spring {@code TestController}.
 */
@Path("/test")
@Produces(MediaType.APPLICATION_JSON)
public class TestResource {

    /** Throws {@link BadRequestException} — expected HTTP 400. */
    @GET
    @Path("/bad-request")
    public void badRequest() {
        throw new BadRequestException();
    }

    /** Throws {@link ResourceNotFoundException} — expected HTTP 404. */
    @GET
    @Path("/not-found")
    public void notFound() {
        throw new ResourceNotFoundException();
    }

    /**
     * Throws {@link RateLimitExceededException} — expected HTTP 429.
     *
     * <p>Uses a fixed window with limit=100, remaining=0 and a reset date one hour in the future.
     */
    @GET
    @Path("/rate-limit")
    public void rateLimit() {
        throw new RateLimitExceededException(100, 0, OffsetDateTime.now().plusHours(1));
    }

    /**
     * Throws {@link ValidationException} with two field rejections — expected HTTP 400
     * with a body containing the validation errors.
     */
    @GET
    @Path("/validation-error")
    public void validationError() {
        final ValidationResult result = new ValidationResult();
        result.rejectValue("name", "name.required");
        result.rejectValue("email", "email.required");
        throw new ValidationException(result);
    }
}
