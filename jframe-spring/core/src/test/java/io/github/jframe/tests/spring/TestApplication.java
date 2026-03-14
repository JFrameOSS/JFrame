package io.github.jframe.tests.spring;

import org.springframework.boot.autoconfigure.SpringBootApplication;

/**
 * Minimal Spring Boot application for integration tests.
 *
 * <p>Component scanning covers {@code io.github.jframe} so that {@code CoreAutoConfiguration}
 * and all JFrame beans (exception handler, enrichers, etc.) are auto-discovered.
 */
@SpringBootApplication(scanBasePackages = "io.github.jframe")
public class TestApplication {
}
