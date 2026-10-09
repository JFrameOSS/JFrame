package io.github.jframe.tests.spring.negotiation;

import io.github.jframe.exception.HttpException;
import io.github.jframe.exception.handler.ErrorResponseWriter;
import io.github.support.ProblemJson;
import io.github.support.fixtures.TestApiError;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import java.util.stream.Stream;
import jakarta.servlet.FilterChain;
import jakarta.servlet.ServletException;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import jakarta.ws.rs.core.Response;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Real embedded server: handler, {@code /error} fallback and {@link ErrorResponseWriter} negotiate the error media type.
 */
@DisplayName("Spring Integration - Problem Details content negotiation")
@SpringBootTest(
    classes = ProblemContentNegotiationIntegrationTest.NegotiationTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ProblemContentNegotiationIntegrationTest {

    private static final String PROBLEM_JSON = ProblemJson.PROBLEM_JSON;
    private static final String JSON = "application/json";

    private static final String HANDLER = "/negotiation/handler";
    private static final String FALLBACK = "/negotiation/send-error";
    private static final String WRITER = "/negotiation/writer";

    @LocalServerPort
    private int port;

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
        final Object[][] paths = {
            {
                HANDLER,
                409,
                "ORDER_CLOSED"
            },
            {
                FALLBACK,
                404,
                "NOT_FOUND"
            },
            {
                WRITER,
                401,
                "TOKEN_EXPIRED"
            }
        };
        final Stream.Builder<Arguments> builder = Stream.builder();
        for (final Object[] path : paths) {
            for (final String[] accept : accepts) {
                builder.add(Arguments.of(path[0], accept[0], accept[1], path[1], path[2]));
            }
        }
        return builder.build();
    }

    @ParameterizedTest(name = "{0} Accept={1} -> {2}")
    @MethodSource("scenarios")
    @DisplayName("Should negotiate problem+json or json with identical body and never 406")
    public void shouldNegotiateErrorMediaType(final String path, final String accept, final String expectedType,
        final int status, final String errorCode) throws Exception {
        // Given: A request with the given Accept header
        final HttpRequest.Builder request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET();
        if (accept != null) {
            request.header("Accept", accept);
        }

        // When: The error is rendered
        final HttpResponse<String> response = HttpClient.newHttpClient().send(request.build(), HttpResponse.BodyHandlers.ofString());
        final Map<String, Object> body = ProblemJson.parse(response.body());

        // Then: Original status, negotiated content type, same Problem Details body
        assertThat(response.statusCode(), is(status));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), startsWith(expectedType));
        ProblemJson.assertRfc9457(response.body(), status);
        assertThat(body, hasEntry("errorCode", errorCode));
        assertThat(body, hasEntry("instance", path));
    }

    /** Errors raised in the MVC handler, via {@code /error} and via {@link ErrorResponseWriter}. */
    @SpringBootApplication
    static class NegotiationTestApplication {

        @Bean
        OncePerRequestFilter negotiationFilter() {
            return new NegotiationFilter();
        }

        @Bean
        NegotiationController negotiationController() {
            return new NegotiationController();
        }

        @Bean
        SecurityFilterChain permitAll(final HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }


    /** Throws from the MVC handler. */
    @RestController
    static class NegotiationController {

        @GetMapping(HANDLER)
        public Map<String, Object> handler() {
            throw new HttpException(new TestApiError("ORDER_CLOSED", "Order is already closed", Response.Status.CONFLICT));
        }
    }


    /** Fails outside the MVC handler chain. */
    static class NegotiationFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, ServletException {
            final String uri = request.getRequestURI();
            if (FALLBACK.equals(uri)) {
                response.sendError(404);
                return;
            }
            if (WRITER.equals(uri)) {
                ErrorResponseWriter.write(
                    request,
                    response,
                    new TestApiError("TOKEN_EXPIRED", "Token has expired", Response.Status.UNAUTHORIZED)
                );
                return;
            }
            chain.doFilter(request, response);
        }
    }
}
