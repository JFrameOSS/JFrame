package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.core.BadRequestException;
import io.github.jframe.exception.core.DataNotFoundException;
import io.github.jframe.exception.core.InternalServerErrorException;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ResourceNotFoundException;
import io.github.jframe.exception.core.UnauthorizedRequestException;
import io.github.jframe.exception.mapper.HttpExceptionMapper;
import io.github.jframe.exception.mapper.RateLimitExceededExceptionMapper;
import io.github.jframe.tests.contract.ContractFixtures;
import io.github.jframe.tests.contract.ContractFixtures.ExceptionScenario;

import java.time.OffsetDateTime;
import java.util.stream.Stream;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.MethodSource;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Quarkus integration tests that verify the exception handling contract.
 *
 * <p>Uses the direct-mapper-invocation approach: each JAX-RS {@code ExceptionMapper} is
 * instantiated directly, its {@code toResponse()} method is called with the appropriate
 * exception instance, and the resulting {@link Response} status is compared against the
 * expected status code from the shared contract fixtures.
 *
 * <p>This approach is framework-agnostic and more rigorous than a full-server test because
 * it tests the mapper logic in isolation, without any container routing or serialisation
 * overhead. It mirrors the pattern used in {@code ExceptionHandlingContractTest} in the
 * {@code jframe-tests-spring} module.
 */
@DisplayName("Quarkus Integration - Exception Handling Contract Tests")
class ExceptionHandlingContractTest {

    /** Mapper for {@code HttpException} subclasses (BadRequest, NotFound, Unauthorized, InternalError). */
    private final HttpExceptionMapper httpExceptionMapper = new HttpExceptionMapper();

    /** Dedicated mapper for {@link RateLimitExceededException} — takes priority over HttpExceptionMapper. */
    private final RateLimitExceededExceptionMapper rateLimitMapper = new RateLimitExceededExceptionMapper();

    /**
     * Provides exception scenarios loaded from the shared contract fixture file.
     *
     * @return stream of {@link ExceptionScenario} instances
     */
    static Stream<ExceptionScenario> exceptionScenarios() {
        return ContractFixtures.loadExceptionScenarios().stream();
    }

    /**
     * Verifies that each exception scenario maps to the correct HTTP status code via the
     * appropriate Quarkus JAX-RS exception mapper.
     *
     * @param scenario the exception scenario from the contract fixtures
     */
    @ParameterizedTest(name = "{0}")
    @MethodSource("exceptionScenarios")
    @DisplayName("Should return correct status code for exception scenario")
    void shouldReturnCorrectStatusForExceptionScenario(final ExceptionScenario scenario) {
        // Given: An exception instance built from the contract scenario
        final Response response = mapScenarioToResponse(scenario);

        // Then: Response status matches expected status code from contract
        assertThat(response.getStatus(), is(scenario.expectedStatusCode()));
    }

    /**
     * Builds the exception from the scenario and routes it to the correct mapper.
     *
     * @param scenario the exception scenario describing the type and message
     * @return the JAX-RS {@link Response} produced by the appropriate mapper
     * @throws IllegalArgumentException if the exception type is unknown
     */
    private Response mapScenarioToResponse(final ExceptionScenario scenario) {
        return switch (scenario.exceptionType()) {
            case "BadRequestException" ->
                httpExceptionMapper.toResponse(new BadRequestException(scenario.message()));
            case "ResourceNotFoundException" ->
                httpExceptionMapper.toResponse(new ResourceNotFoundException(scenario.message()));
            case "DataNotFoundException" ->
                httpExceptionMapper.toResponse(new DataNotFoundException(scenario.message()));
            case "UnauthorizedRequestException" ->
                httpExceptionMapper.toResponse(new UnauthorizedRequestException(scenario.message()));
            case "InternalServerErrorException" ->
                httpExceptionMapper.toResponse(new InternalServerErrorException(scenario.message()));
            case "RateLimitExceededException" ->
                rateLimitMapper.toResponse(new RateLimitExceededException(100, 0, OffsetDateTime.now().plusHours(1)));
            default ->
                throw new IllegalArgumentException("Unknown exception type: " + scenario.exceptionType());
        };
    }
}
