package io.github.jframe.tracing.enricher;

import io.github.jframe.exception.core.BadRequestException;
import io.github.support.ProblemJson;

import java.net.URI;
import java.net.http.HttpClient;
import java.net.http.HttpRequest;
import java.net.http.HttpResponse;
import java.util.Map;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.hasEntry;
import static org.hamcrest.Matchers.matchesPattern;

/**
 * Full Spring context with OTLP enabled: error bodies carry the active trace and span ids.
 */
@DisplayName("Spring OTLP Integration - traceId/spanId in Problem Details")
@SpringBootTest(
    classes = TracingProblemDetailsIntegrationTest.TracingTestApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT,
    properties = {
        "jframe.otlp.disabled=false",
        "otel.sdk.disabled=false",
        "otel.traces.exporter=none",
        "otel.metrics.exporter=none",
        "otel.logs.exporter=none",
        "otel.traces.sampler=always_on"
    }
)
public class TracingProblemDetailsIntegrationTest {

    private static final String HEX_32 = "[0-9a-f]{32}";
    private static final String HEX_16 = "[0-9a-f]{16}";

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should add traceId and spanId when OTLP is enabled")
    public void shouldAddTraceAndSpanIdWhenOtlpEnabled() throws Exception {
        // Given: A running application with OTLP tracing
        final HttpRequest request = HttpRequest.newBuilder(URI.create("http://localhost:" + port + "/traced/bad-request")).GET().build();

        // When: An endpoint fails
        final HttpResponse<String> response = HttpClient.newHttpClient().send(request, HttpResponse.BodyHandlers.ofString());
        final Map<String, Object> body = ProblemJson.parse(response.body());

        // Then: Conformant body with W3C trace and span ids
        ProblemJson.assertRfc9457(response.body(), response.statusCode());
        assertThat(body, hasEntry("status", 400));
        assertThat((String) body.get("traceId"), matchesPattern(HEX_32));
        assertThat((String) body.get("spanId"), matchesPattern(HEX_16));
    }

    /** Minimal application with a failing endpoint and open security. */
    @SpringBootApplication
    @RestController
    static class TracingTestApplication {

        @GetMapping("/traced/bad-request")
        public void badRequest() {
            throw new BadRequestException();
        }

        @Bean
        SecurityFilterChain permitAll(final HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }
}
