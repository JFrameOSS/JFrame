package io.github.jframe.tests.spring;

import org.springframework.boot.test.context.TestConfiguration;
import org.springframework.context.annotation.Bean;
import org.springframework.security.config.annotation.web.builders.HttpSecurity;
import org.springframework.security.web.SecurityFilterChain;

/**
 * Shared test security configuration that permits all requests without authentication.
 *
 * <p>Extracted to a standalone {@link TestConfiguration} so that both
 * {@link ExceptionHandlingContractTest} and {@link ValidationContractTest} can share
 * the same Spring application context via context caching, avoiding duplicate bean definitions.
 */
@TestConfiguration
public class TestSecurityConfiguration {

    /**
     * Creates a permissive {@link SecurityFilterChain} that disables CSRF and allows all requests.
     *
     * <p>Security behaviour is outside the scope of exception-handling and validation contract tests.
     *
     * @param http the {@link HttpSecurity} to configure
     * @return the built filter chain
     * @throws Exception if configuration fails
     */
    @Bean
    public SecurityFilterChain testFilterChain(final HttpSecurity http) throws Exception {
        return http
            .csrf(csrf -> csrf.disable())
            .authorizeHttpRequests(auth -> auth.anyRequest().permitAll())
            .build();
    }
}
