package io.github.jframe.autoconfigure.properties;

import io.github.support.UnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.lang.reflect.Method;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;
import static org.hamcrest.Matchers.nullValue;

/**
 * Tests for {@link ApplicationConfig} — Quarkus {@code @ConfigMapping} interface.
 *
 * <p>Verifies via reflection that:
 * <ul>
 * <li>The interface carries the correct {@code @ConfigMapping} annotation and prefix</li>
 * <li>Each method exists and returns the correct type</li>
 * <li>The {@code environment()} method is annotated with {@code @WithDefault("dev")}</li>
 * </ul>
 *
 * <p>These tests are intentionally annotation/contract-only; SmallRye Config runtime binding
 * is outside the scope of unit tests and is covered by Quarkus integration tests.
 */
@DisplayName("Quarkus Core - Application Config")
public class ApplicationConfigTest extends UnitTest {

    // -------------------------------------------------------------------------
    // @ConfigMapping annotation — interface-level contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be annotated with @ConfigMapping")
    public void shouldBeAnnotatedWithConfigMapping() {
        // Given: The ApplicationConfig interface class

        // When: Retrieving the @ConfigMapping annotation
        final ConfigMapping annotation = ApplicationConfig.class.getAnnotation(ConfigMapping.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @ConfigMapping prefix 'jframe.application'")
    public void shouldHaveConfigMappingPrefixJframeApplication() {
        // Given: The @ConfigMapping annotation on the interface

        // When: Reading the prefix attribute
        final String prefix = ApplicationConfig.class.getAnnotation(ConfigMapping.class).prefix();

        // Then: Prefix is 'jframe.application'
        assertThat(prefix, is("jframe.application"));
    }

    @Test
    @DisplayName("Should be an interface")
    public void shouldBeAnInterface() {
        // Given: The ApplicationConfig class object

        // When: Checking if it is an interface
        final boolean isInterface = ApplicationConfig.class.isInterface();

        // Then: It must be an interface
        assertThat(isInterface, is(true));
    }

    // -------------------------------------------------------------------------
    // name() — String, no default
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'name' method returning String")
    public void shouldDeclareNameMethodReturningString() throws NoSuchMethodException {
        // Given: The ApplicationConfig interface

        // When: Looking up the 'name' method
        final Method method = ApplicationConfig.class.getMethod("name");

        // Then: Return type must be String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should not annotate 'name' with @WithDefault")
    public void shouldNotAnnotateNameWithWithDefault() throws NoSuchMethodException {
        // Given: The 'name' method on ApplicationConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            ApplicationConfig.class.getMethod("name").getAnnotation(WithDefault.class);

        // Then: No @WithDefault — name is required
        assertThat(annotation, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // group() — String, no default
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'group' method returning String")
    public void shouldDeclareGroupMethodReturningString() throws NoSuchMethodException {
        // Given: The ApplicationConfig interface

        // When: Looking up the 'group' method
        final Method method = ApplicationConfig.class.getMethod("group");

        // Then: Return type must be String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should not annotate 'group' with @WithDefault")
    public void shouldNotAnnotateGroupWithWithDefault() throws NoSuchMethodException {
        // Given: The 'group' method on ApplicationConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            ApplicationConfig.class.getMethod("group").getAnnotation(WithDefault.class);

        // Then: No @WithDefault — group is required
        assertThat(annotation, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // version() — String, no default
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'version' method returning String")
    public void shouldDeclareVersionMethodReturningString() throws NoSuchMethodException {
        // Given: The ApplicationConfig interface

        // When: Looking up the 'version' method
        final Method method = ApplicationConfig.class.getMethod("version");

        // Then: Return type must be String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should not annotate 'version' with @WithDefault")
    public void shouldNotAnnotateVersionWithWithDefault() throws NoSuchMethodException {
        // Given: The 'version' method on ApplicationConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            ApplicationConfig.class.getMethod("version").getAnnotation(WithDefault.class);

        // Then: No @WithDefault — version is required
        assertThat(annotation, is(nullValue()));
    }

    // -------------------------------------------------------------------------
    // environment() — String, default "dev"
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'environment' method returning String")
    public void shouldDeclareEnvironmentMethodReturningString() throws NoSuchMethodException {
        // Given: The ApplicationConfig interface

        // When: Looking up the 'environment' method
        final Method method = ApplicationConfig.class.getMethod("environment");

        // Then: Return type must be String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should annotate 'environment' with @WithDefault")
    public void shouldAnnotateEnvironmentWithWithDefault() throws NoSuchMethodException {
        // Given: The 'environment' method on ApplicationConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            ApplicationConfig.class.getMethod("environment").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault('dev') on 'environment'")
    public void shouldHaveWithDefaultDevOnEnvironment() throws NoSuchMethodException {
        // Given: The 'environment' method on ApplicationConfig

        // When: Reading the default value
        final String defaultValue =
            ApplicationConfig.class.getMethod("environment").getAnnotation(WithDefault.class).value();

        // Then: Default is 'dev'
        assertThat(defaultValue, is("dev"));
    }

    // -------------------------------------------------------------------------
    // Interface method count — ensures no accidental extra/missing methods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare exactly 4 methods")
    public void shouldDeclareExactlyFourMethods() {
        // Given: The ApplicationConfig interface

        // When: Counting declared methods
        final int methodCount = ApplicationConfig.class.getDeclaredMethods().length;

        // Then: Exactly 4 methods are declared (name, group, version, environment)
        assertThat(methodCount, is(4));
    }
}
