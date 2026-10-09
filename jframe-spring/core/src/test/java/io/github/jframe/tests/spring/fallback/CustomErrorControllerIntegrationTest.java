package io.github.jframe.tests.spring.fallback;

import java.net.http.HttpResponse;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.SpringBootApplication;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.boot.webmvc.error.ErrorController;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.filter.OncePerRequestFilter;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * jFrame's {@code /error} fallback backs off when the application defines its own {@link ErrorController}.
 */
@DisplayName("Spring Integration - /error fallback backs off for custom ErrorController")
@SpringBootTest(
    classes = CustomErrorControllerIntegrationTest.CustomErrorApplication.class,
    webEnvironment = SpringBootTest.WebEnvironment.RANDOM_PORT
)
public class CustomErrorControllerIntegrationTest {

    @LocalServerPort
    private int port;

    @Test
    @DisplayName("Should use the application's ErrorController when one is defined")
    public void shouldUseApplicationErrorControllerWhenDefined() throws Exception {
        // Given: An application-defined ErrorController
        // When: A filter sends 404
        final HttpResponse<String> response = ErrorFallbackIntegrationTest.get(port, "/fallback/send-error/404");

        // Then: The application's body is rendered
        assertThat(response.body(), is("custom-error"));
    }

    /** Application with its own {@code /error} controller. */
    @SpringBootApplication
    @RestController
    static class CustomErrorApplication implements ErrorController {

        @RequestMapping("/error")
        public String error() {
            return "custom-error";
        }

        @Bean
        OncePerRequestFilter failingFilter() {
            return new ErrorFallbackIntegrationTest.FailingFilter();
        }

        @Bean
        SecurityFilterChain permitAll(final HttpSecurity http) throws Exception {
            return http.csrf(csrf -> csrf.disable()).authorizeHttpRequests(auth -> auth.anyRequest().permitAll()).build();
        }
    }
}
