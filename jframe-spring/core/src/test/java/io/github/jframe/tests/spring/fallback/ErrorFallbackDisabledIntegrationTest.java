package io.github.jframe.tests.spring.fallback;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.*;

/**
 * With {@code jframe.exception.enabled=false} Boot's default {@code /error} handling applies.
 */
@DisplayName("Spring Integration - /error fallback disabled")
@SpringBootTest(
    classes = ErrorFallbackIntegrationTest.FallbackTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = "jframe.exception.enabled=false"
)
public class ErrorFallbackDisabledIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should use Boot default error body when jFrame exception handling is disabled")
    public void shouldUseBootDefaultWhenDisabled() throws Exception {
        // Given: jFrame exception handling disabled
        // When: A filter sends 404
        final HttpResponse<String> response = ErrorFallbackIntegrationTest.get(port, "/fallback/send-error/404");

        // Then: Boot's default body, not jFrame Problem Details
        assertThat(response.statusCode(), is(404));
        assertThat(response.headers().firstValue("Content-Type").orElse(""), not(startsWith("application/problem+json")));
        assertThat(response.body(), not(containsString("errorCode")));
    }
}
