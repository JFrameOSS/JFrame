package io.github.jframe.tracing;

import io.github.support.UnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.lang.reflect.Method;
import java.util.Set;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link OpenTelemetryConfig} — verifies the @ConfigMapping interface contract
 * by inspecting annotations and method signatures via reflection.
 *
 * <p>Since SmallRye Config default values are only applied at runtime via CDI/injection,
 * these tests validate the annotation-based contract without starting a Quarkus context.
 */
@DisplayName("Quarkus OTLP - OpenTelemetry Config")
public class OpenTelemetryConfigTest extends UnitTest {

    // -------------------------------------------------------------------------
    // @ConfigMapping prefix
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be annotated with @ConfigMapping")
    public void shouldBeAnnotatedWithConfigMapping() {
        // Given: The OpenTelemetryConfig interface class

        // When: Inspecting its @ConfigMapping annotation
        final ConfigMapping configMapping = OpenTelemetryConfig.class.getAnnotation(ConfigMapping.class);

        // Then: @ConfigMapping is present
        assertThat(configMapping, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have prefix 'jframe.otlp' on @ConfigMapping")
    public void shouldHavePrefixJframeOtlpOnConfigMapping() {
        // Given: The @ConfigMapping annotation on OpenTelemetryConfig

        // When: Reading the prefix value
        final ConfigMapping configMapping = OpenTelemetryConfig.class.getAnnotation(ConfigMapping.class);

        // Then: The prefix is exactly "jframe.otlp"
        assertThat(configMapping.prefix(), is(equalTo("jframe.otlp")));
    }

    // -------------------------------------------------------------------------
    // disabled()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'disabled' method returning boolean")
    public void shouldHaveDisabledMethodReturningBoolean() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the disabled() method
        final Method method = OpenTelemetryConfig.class.getMethod("disabled");

        // Then: Return type is boolean
        assertThat(method.getReturnType(), is(equalTo(boolean.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('false') on 'disabled' method")
    public void shouldHaveWithDefaultFalseOnDisabledMethod() throws NoSuchMethodException {
        // Given: The disabled() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("disabled");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "false"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("false")));
    }

    // -------------------------------------------------------------------------
    // url()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'url' method returning String")
    public void shouldHaveUrlMethodReturningString() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the url() method
        final Method method = OpenTelemetryConfig.class.getMethod("url");

        // Then: Return type is String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('http://localhost:4318') on 'url' method")
    public void shouldHaveWithDefaultLocalhostOtlpPortOnUrlMethod() throws NoSuchMethodException {
        // Given: The url() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("url");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "http://localhost:4318"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("http://localhost:4318")));
    }

    // -------------------------------------------------------------------------
    // timeout()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'timeout' method returning String")
    public void shouldHaveTimeoutMethodReturningString() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the timeout() method
        final Method method = OpenTelemetryConfig.class.getMethod("timeout");

        // Then: Return type is String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('10s') on 'timeout' method")
    public void shouldHaveWithDefault10sOnTimeoutMethod() throws NoSuchMethodException {
        // Given: The timeout() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("timeout");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "10s"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("10s")));
    }

    // -------------------------------------------------------------------------
    // exporter()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'exporter' method returning String")
    public void shouldHaveExporterMethodReturningString() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the exporter() method
        final Method method = OpenTelemetryConfig.class.getMethod("exporter");

        // Then: Return type is String
        assertThat(method.getReturnType(), is(equalTo(String.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('otlp') on 'exporter' method")
    public void shouldHaveWithDefaultOtlpOnExporterMethod() throws NoSuchMethodException {
        // Given: The exporter() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("exporter");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "otlp"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("otlp")));
    }

    // -------------------------------------------------------------------------
    // samplingRate()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'samplingRate' method returning double")
    public void shouldHaveSamplingRateMethodReturningDouble() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the samplingRate() method
        final Method method = OpenTelemetryConfig.class.getMethod("samplingRate");

        // Then: Return type is double
        assertThat(method.getReturnType(), is(equalTo(double.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('1.0') on 'samplingRate' method")
    public void shouldHaveWithDefault1Point0OnSamplingRateMethod() throws NoSuchMethodException {
        // Given: The samplingRate() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("samplingRate");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "1.0"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("1.0")));
    }

    // -------------------------------------------------------------------------
    // excludedMethods()
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should have 'excludedMethods' method returning Set")
    public void shouldHaveExcludedMethodsMethodReturningSet() throws NoSuchMethodException {
        // Given: The OpenTelemetryConfig interface

        // When: Retrieving the excludedMethods() method
        final Method method = OpenTelemetryConfig.class.getMethod("excludedMethods");

        // Then: Return type is Set
        assertThat(method.getReturnType(), is(equalTo(Set.class)));
    }

    @Test
    @DisplayName("Should have @WithDefault('health,actuator,ping,status') on 'excludedMethods' method")
    public void shouldHaveWithDefaultHealthActuatorPingStatusOnExcludedMethodsMethod() throws NoSuchMethodException {
        // Given: The excludedMethods() method on OpenTelemetryConfig

        // When: Inspecting its @WithDefault annotation
        final Method method = OpenTelemetryConfig.class.getMethod("excludedMethods");
        final WithDefault withDefault = method.getAnnotation(WithDefault.class);

        // Then: @WithDefault is present with value "health,actuator,ping,status"
        assertThat(withDefault, is(notNullValue()));
        assertThat(withDefault.value(), is(equalTo("health,actuator,ping,status")));
    }

    // -------------------------------------------------------------------------
    // Interface contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be an interface")
    public void shouldBeAnInterface() {
        // Given: The OpenTelemetryConfig type

        // When: Checking if it is an interface

        // Then: It is an interface
        assertThat(OpenTelemetryConfig.class.isInterface(), is(true));
    }

    @Test
    @DisplayName("Should declare exactly six configuration methods")
    public void shouldDeclareExactlySixConfigurationMethods() {
        // Given: The OpenTelemetryConfig interface

        // When: Counting declared methods
        final int methodCount = OpenTelemetryConfig.class.getDeclaredMethods().length;

        // Then: Exactly six methods are declared
        assertThat(methodCount, is(equalTo(6)));
    }
}
