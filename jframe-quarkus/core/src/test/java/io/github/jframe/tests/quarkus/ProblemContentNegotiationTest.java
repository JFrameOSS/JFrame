package io.github.jframe.tests.quarkus;

import io.github.jframe.exception.HttpException;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;

import java.util.Arrays;
import java.util.List;
import java.util.stream.Stream;
import jakarta.ws.rs.NotFoundException;
import jakarta.ws.rs.container.ContainerRequestContext;
import jakarta.ws.rs.core.HttpHeaders;
import jakarta.ws.rs.core.MediaType;
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
import static org.mockito.Mockito.when;

/**
 * Mappers negotiate {@code application/problem+json} vs {@code application/json} from the request Accept header; never 406.
 */
@DisplayName("Quarkus Integration - Problem Details content negotiation")
class ProblemContentNegotiationTest {

    private static final String PROBLEM_JSON = ProblemJson.PROBLEM_JSON;
    private static final String JSON = MediaType.APPLICATION_JSON;

    static Stream<Arguments> scenarios() {
        final String[][] accepts = {
            {
                null,
                PROBLEM_JSON
            },
            {
                "*/*",
                PROBLEM_JSON
            },
            {
                PROBLEM_JSON,
                PROBLEM_JSON
            },
            {
                JSON,
                JSON
            },
            {
                "application/json;q=1",
                JSON
            },
            {
                "application/json, application/problem+json;q=0.1",
                PROBLEM_JSON
            },
            {
                "application/*",
                PROBLEM_JSON
            },
            {
                "text/html",
                PROBLEM_JSON
            }
        };
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final String[] accept : accepts) {
            builder.add(
                Arguments.of(
                    new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT)),
                    accept[0],
                    accept[1],
                    409,
                    "ORDER_CLOSED"
                )
            );
            builder.add(Arguments.of(new NotFoundException(), accept[0], accept[1], 404, "NOT_FOUND"));
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0} Accept={1} -> {2}")
    @MethodSource("scenarios")
    @DisplayName("Should negotiate problem+json or json with identical body and never 406")
    void shouldNegotiateErrorMediaType(final Exception exception, final String accept, final String expectedType, final int status,
        final String errorCode) {
        // Given: A request with the given Accept header
        final ContainerRequestContext request = aRequestAccepting("/test/negotiation", accept);

        // When: Mapping the exception
        final Response response = QuarkusProblems.map(exception, aBuilder(builtInEnrichersPlus()), request);

        // Then: Original status, negotiated media type, same Problem Details body
        assertThat(response.getStatus(), is(status));
        assertThat(response.getMediaType().toString(), startsWith(expectedType));
        assertThat(ProblemJson.parse(QuarkusProblems.conformantJson(response)), hasEntry("errorCode", errorCode));
    }

    private static ContainerRequestContext aRequestAccepting(final String path, final String accept) {
        final ContainerRequestContext request = aRequest(path);
        final List<MediaType> acceptable = accept == null
            ? List.of(MediaType.WILDCARD_TYPE)
            : Arrays.stream(accept.split(",")).map(String::trim).map(MediaType::valueOf).toList();
        when(request.getHeaderString(HttpHeaders.ACCEPT)).thenReturn(accept);
        when(request.getAcceptableMediaTypes()).thenReturn(acceptable);
        return request;
    }
}
