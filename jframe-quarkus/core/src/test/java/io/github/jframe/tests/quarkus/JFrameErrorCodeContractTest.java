package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.JFrameErrorCode;
import io.github.jframe.exception.core.RateLimitExceededException;
import io.github.jframe.exception.core.ValidationException;
import io.github.jframe.exception.page.InvalidPageException;
import io.github.jframe.exception.search.InvalidSearchException;
import io.github.jframe.exception.sort.InvalidSortException;
import io.github.jframe.validation.ValidationResult;
import io.github.support.ProblemJson;

import java.time.OffsetDateTime;
import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import jakarta.ws.rs.BadRequestException;
import jakarta.ws.rs.ForbiddenException;
import jakarta.ws.rs.NotAcceptableException;
import jakarta.ws.rs.NotAllowedException;
import jakarta.ws.rs.NotAuthorizedException;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.NotSupportedException;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import static io.github.jframe.exception.factory.ProblemDetailsFixture.aBuilder;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.aRequest;
import static io.github.jframe.exception.factory.ProblemDetailsFixture.builtInEnrichersPlus;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Every errorCode emitted for JAX-RS framework / security / jFrame errors is a {@link JFrameErrorCode} constant.
 */
@DisplayName("Quarkus Integration - Emitted errorCodes are JFrameErrorCodes")
class JFrameErrorCodeContractTest {

    static Stream<Arguments> scenarios() {
        final ValidationResult validation = new ValidationResult();
        validation.rejectValue("name", "name.required");
        return Stream.of(
            Arguments.of(new BadRequestException(), 400, "BAD_REQUEST"),
            Arguments.of(new NotAuthorizedException("Bearer"), 401, "UNAUTHORIZED"),
            Arguments.of(new ForbiddenException(), 403, "FORBIDDEN"),
            Arguments.of(new NotFoundException(), 404, "NOT_FOUND"),
            Arguments.of(new NotAllowedException("GET"), 405, "METHOD_NOT_ALLOWED"),
            Arguments.of(new NotAcceptableException(), 406, "NOT_ACCEPTABLE"),
            Arguments.of(new NotSupportedException(), 415, "UNSUPPORTED_MEDIA_TYPE"),
            Arguments.of(new ValidationException(validation), 400, "VALIDATION_ERROR"),
            Arguments.of(new InvalidSortException("password", List.of("name")), 400, "INVALID_SORT"),
            Arguments.of(new InvalidSearchException("age", "abc", List.of("age")), 400, "INVALID_SEARCH"),
            Arguments.of(new InvalidPageException("size", -1), 400, "INVALID_PAGE"),
            Arguments.of(new RateLimitExceededException(100, 0, OffsetDateTime.parse("2030-01-01T12:00:00Z")), 429, "RATE_LIMIT_EXCEEDED"),
            Arguments.of(new IllegalStateException("boom"), 500, "INTERNAL_SERVER_ERROR")
        );
    }

    @ParameterizedTest(name = "{0} -> {2}")
    @MethodSource("scenarios")
    @DisplayName("Should emit a JFrameErrorCode name matching the status")
    void shouldEmitJFrameErrorCode(final Throwable exception, final int status, final String errorCode) {
        // Given: A framework, security or jFrame exception
        // When: Mapping it as the runtime would
        final Response response = QuarkusProblems.map(exception, aBuilder(builtInEnrichersPlus()), aRequest("/test/codes"));

        // Then: errorCode is the expected JFrameErrorCode with matching status
        final String[] names = Arrays.stream(JFrameErrorCode.values()).map(Enum::name).toArray(String[]::new);
        assertThat(response.getStatus(), is(status));
        assertThat(ProblemJson.parse(QuarkusProblems.conformantJson(response)), hasEntry("errorCode", errorCode));
        assertThat(errorCode, is(oneOf(names)));
        assertThat(JFrameErrorCode.valueOf(errorCode).getHttpStatus().getStatusCode(), is(status));
    }
}
