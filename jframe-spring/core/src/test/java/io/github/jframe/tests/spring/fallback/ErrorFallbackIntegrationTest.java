package io.github.jframe.tests.spring.fallback;

import io.github.support.ProblemJson;

import java.io.IOException;
import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;
import jakarta.servlet.FilterChain;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.filter.OncePerRequestFilter;

import static io.github.jframe.util.constants.Constants.Headers.TX_ID_HEADER;
import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * Real embedded server: errors raised outside the MVC handler reach {@code /error} and must still render jFrame Problem Details.
 */
@DisplayName("Spring Integration - /error fallback renders Problem Details")
@SpringBootTest(
    classes = ErrorFallbackIntegrationTest.FallbackTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class ErrorFallbackIntegrationTest {

    static final String SECRET = "jdbc:postgresql://db/secret";

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should render generic 500 Problem Details when a servlet filter throws")
    public void shouldRenderGeneric500WhenFilterThrows() throws Exception {
        // Given: A filter that throws with a sensitive message
        // When: Calling the path
        final HttpResponse<String> response = get(port, "/fallback/throw");
        final Map<String, Object> body = ProblemJson.parse(response.body());

        // Then: Generic, conformant 500 for the original path, no leaked message
        assertProblem(response, 500);
        assertThat(body, hasEntry("errorCode", "INTERNAL_SERVER_ERROR"));
        assertThat(body, hasEntry("detail", "Internal server error"));
        assertThat(body, hasEntry("instance", "/fallback/throw"));
        assertThat(body, not(hasKey("txId")));
        assertThat(response.body(), not(containsString(SECRET)));
        assertThat(response.body(), not(containsString("IllegalStateException")));
    }

    @Test
    @DisplayName("Should echo the request transaction id when present")
    public void shouldEchoRequestTransactionId() throws Exception {
        // Given: A request carrying a transaction id header
        final String txId = "3f1c2b7a-1d2e-4f5a-9b8c-0d1e2f3a4b5c";
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/fallback/throw"))
            .header(TX_ID_HEADER, txId)
            .GET()
            .build();

        // When: Calling a failing path
        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());

        // Then: The body reports that transaction id
        assertProblem(response, 500);
        assertThat(ProblemJson.parse(response.body()), hasEntry("txId", txId));
    }

    @ParameterizedTest(name = "sendError({0}) -> {1}")
    @CsvSource(
        {
            "404, NOT_FOUND, Not Found",
            "403, FORBIDDEN, Forbidden"
        }
    )
    @DisplayName("Should render Problem Details when a filter calls sendError without an exception")
    public void shouldRenderProblemDetailsWhenFilterSendsError(final int status, final String errorCode, final String reason)
        throws Exception {
        // Given: A filter that only calls response.sendError(status)
        final String path = "/fallback/send-error/" + status;

        // When: Calling the path
        final HttpResponse<String> response = get(port, path);
        final Map<String, Object> body = ProblemJson.parse(response.body());

        // Then: Status-named errorCode, reason-phrase detail, original path as instance
        assertProblem(response, status);
        assertThat(body, hasEntry("errorCode", errorCode));
        assertThat(body, hasEntry("title", reason));
        assertThat(body, hasEntry("detail", reason));
        assertThat(body, hasEntry("instance", path));
        assertThat(body, not(hasKey("type")));
    }

    @Test
    @DisplayName("Should leave an already committed response untouched")
    public void shouldLeaveCommittedResponseUntouched() throws Exception {
        // Given: A filter that commits a 200 body, then throws
        // When: Calling the path
        final HttpResponse<String> response = get(port, "/fallback/committed");

        // Then: The committed response is kept, no Problem Details appended
        assertThat(response.statusCode(), is(200));
        assertThat(response.body(), is("partial"));
        assertThat(response.body(), not(containsString("errorCode")));
    }

    static HttpResponse<String> get(final int port, final String path) throws Exception {
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + path)).GET().build();
        return HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
    }

    private static void assertProblem(final HttpResponse<String> response, final int status) {
        assertThat(response.statusCode(), is(status));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), startsWith(ProblemJson.PROBLEM_JSON));
        ProblemJson.assertRfc9457(response.body(), status);
        assertThat(ProblemJson.parse(response.body()), hasEntry("status", status));
    }

    /** Minimal application whose filter fails before reaching any MVC handler. */
    @SpringBootApplication
    static class FallbackTestApplication {

        @Bean
        OncePerRequestFilter failingFilter() {
            return new FailingFilter();
        }

        @Bean
        SecurityFilterChain permitAll(final HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }


    /** Fails requests under {@code /fallback/**} outside the MVC handler chain. */
    static class FailingFilter extends OncePerRequestFilter {

        @Override
        protected void doFilterInternal(final HttpServletRequest request, final HttpServletResponse response, final FilterChain chain)
            throws IOException, jakarta.servlet.ServletException {
            final String uri = request.getRequestURI();
            if ("/fallback/throw".equals(uri)) {
                throw new IllegalStateException(SECRET);
            }
            if (uri.startsWith("/fallback/send-error/")) {
                response.sendError(Integer.parseInt(uri.substring(uri.lastIndexOf('/') + 1)));
                return;
            }
            if ("/fallback/committed".equals(uri)) {
                response.setContentLength("partial".length());
                response.getWriter().write("partial");
                response.flushBuffer();
                throw new IllegalStateException(SECRET);
            }
            chain.doFilter(request, response);
        }
    }
}
