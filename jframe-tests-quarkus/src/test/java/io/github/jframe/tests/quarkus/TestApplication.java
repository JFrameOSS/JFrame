package io.github.jframe.tests.quarkus;

import jakarta.ws.rs.ApplicationPath;
import jakarta.ws.rs.core.Application;

/**
 * JAX-RS {@link Application} subclass that serves as the entry point for the Quarkus test module.
 *
 * <p>Declares the base path {@code /} so that all test resources are reachable without an
 * additional prefix. This mirrors the role of {@code @SpringBootApplication} in the Spring
 * test module: it provides a minimal application skeleton so that the JAX-RS providers and
 * resources are registered correctly when running inside a JAX-RS container.
 *
 * <p>Note: in the direct-mapper-invocation test approach this class is present for completeness
 * and future full-server tests, but is not used by the current parametrised test suite.
 */
@ApplicationPath("/")
public class TestApplication extends Application {
}
