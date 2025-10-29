package io.github.jframe.autoconfigure.properties;

import io.github.support.UnitTest;

import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link OpenTelemetryProperties}.
 *
 * <p>Verifies the OpenTelemetryProperties functionality including:
 * <ul>
 * <li>Validation constraints for required fields</li>
 * <li>Default values</li>
 * <li>Property getters and setters</li>
 * <li>Pattern validation for timeout and exporter</li>
 * <li>Range validation for sampling rate</li>
 * </ul>
 */
@DisplayName("Properties - OpenTelemetryProperties")
class OpenTelemetryPropertiesTest extends UnitTest {

    private Validator validator;
    private OpenTelemetryProperties properties;

    @Override
    @BeforeEach
    public void setUp() {
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        properties = new OpenTelemetryProperties();
    }

    @Test
    @DisplayName("Should pass validation with default values")
    void validate_withDefaults_shouldPassValidation() {
        // Given: OpenTelemetryProperties with default values

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should have default disabled value of false")
    void isDisabled_withDefaults_shouldReturnFalse() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting disabled value
        final boolean disabled = properties.isDisabled();

        // Then: Default value is false
        assertThat(disabled).isFalse();
    }

    @Test
    @DisplayName("Should have default url value of http://localhost:4318")
    void getUrl_withDefaults_shouldReturnLocalhost() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting url value
        final String url = properties.getUrl();

        // Then: Default value is http://localhost:4318
        assertThat(url).isEqualTo("http://localhost:4318");
    }

    @Test
    @DisplayName("Should have default timeout value of 10s")
    void getTimeout_withDefaults_shouldReturn10s() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting timeout value
        final String timeout = properties.getTimeout();

        // Then: Default value is 10s
        assertThat(timeout).isEqualTo("10s");
    }

    @Test
    @DisplayName("Should have default exporter value of otlp")
    void getExporter_withDefaults_shouldReturnOtlp() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting exporter value
        final String exporter = properties.getExporter();

        // Then: Default value is otlp
        assertThat(exporter).isEqualTo("otlp");
    }

    @Test
    @DisplayName("Should have default samplingRate value of 1.0")
    void getSamplingRate_withDefaults_shouldReturn1() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting samplingRate value
        final double samplingRate = properties.getSamplingRate();

        // Then: Default value is 1.0
        assertThat(samplingRate).isEqualTo(1.0);
    }

    @Test
    @DisplayName("Should have default excludedMethods including health, actuator, ping, and status")
    void getExcludedMethods_withDefaults_shouldIncludeCommonMethods() {
        // Given: OpenTelemetryProperties with default values

        // When: Getting excludedMethods value
        final Set<String> excludedMethods = properties.getExcludedMethods();

        // Then: Default set includes common health check methods
        assertThat(excludedMethods)
            .contains("health", "actuator", "ping", "status");
    }

    @Test
    @DisplayName("Should fail validation when url is null")
    void validate_withNullUrl_shouldFailValidation() {
        // Given: OpenTelemetryProperties with null url
        properties.setUrl(null);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank url
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("OpenTelemetry OTLP URL must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when url is blank")
    void validate_withBlankUrl_shouldFailValidation() {
        // Given: OpenTelemetryProperties with blank url
        properties.setUrl("   ");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank url
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("OpenTelemetry OTLP URL must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when timeout is null")
    void validate_withNullTimeout_shouldFailValidation() {
        // Given: OpenTelemetryProperties with null timeout
        properties.setTimeout(null);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank timeout
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("OpenTelemetry timeout must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when timeout is blank")
    void validate_withBlankTimeout_shouldFailValidation() {
        // Given: OpenTelemetryProperties with blank timeout
        properties.setTimeout("   ");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should fail validation when timeout has invalid format")
    void validate_withInvalidTimeoutFormat_shouldFailValidation() {
        // Given: OpenTelemetryProperties with invalid timeout format
        properties.setTimeout("10");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about format
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Timeout must be in format: number followed by s (seconds), m (minutes), or h (hours)");
    }

    @Test
    @DisplayName("Should pass validation when timeout uses seconds format")
    void validate_withSecondsTimeout_shouldPassValidation() {
        // Given: OpenTelemetryProperties with timeout in seconds
        properties.setTimeout("30s");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when timeout uses minutes format")
    void validate_withMinutesTimeout_shouldPassValidation() {
        // Given: OpenTelemetryProperties with timeout in minutes
        properties.setTimeout("5m");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when timeout uses hours format")
    void validate_withHoursTimeout_shouldPassValidation() {
        // Given: OpenTelemetryProperties with timeout in hours
        properties.setTimeout("2h");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when exporter is null")
    void validate_withNullExporter_shouldFailValidation() {
        // Given: OpenTelemetryProperties with null exporter
        properties.setExporter(null);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank exporter
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("OpenTelemetry exporter must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when exporter is blank")
    void validate_withBlankExporter_shouldFailValidation() {
        // Given: OpenTelemetryProperties with blank exporter
        properties.setExporter("   ");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails
        assertThat(violations).hasSizeGreaterThanOrEqualTo(1);
    }

    @Test
    @DisplayName("Should fail validation when exporter is invalid")
    void validate_withInvalidExporter_shouldFailValidation() {
        // Given: OpenTelemetryProperties with invalid exporter
        properties.setExporter("invalid");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about valid options
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Exporter must be one of: otlp, jaeger, zipkin");
    }

    @Test
    @DisplayName("Should pass validation with otlp exporter")
    void validate_withOtlpExporter_shouldPassValidation() {
        // Given: OpenTelemetryProperties with otlp exporter
        properties.setExporter("otlp");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with jaeger exporter")
    void validate_withJaegerExporter_shouldPassValidation() {
        // Given: OpenTelemetryProperties with jaeger exporter
        properties.setExporter("jaeger");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation with zipkin exporter")
    void validate_withZipkinExporter_shouldPassValidation() {
        // Given: OpenTelemetryProperties with zipkin exporter
        properties.setExporter("zipkin");

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when samplingRate is below 0.0")
    void validate_withNegativeSamplingRate_shouldFailValidation() {
        // Given: OpenTelemetryProperties with negative samplingRate
        properties.setSamplingRate(-0.1);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about range
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Sampling rate must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("Should fail validation when samplingRate is above 1.0")
    void validate_withSamplingRateAboveOne_shouldFailValidation() {
        // Given: OpenTelemetryProperties with samplingRate above 1.0
        properties.setSamplingRate(1.1);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about range
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Sampling rate must be between 0.0 and 1.0");
    }

    @Test
    @DisplayName("Should pass validation when samplingRate is 0.0")
    void validate_withZeroSamplingRate_shouldPassValidation() {
        // Given: OpenTelemetryProperties with samplingRate 0.0
        properties.setSamplingRate(0.0);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when samplingRate is 1.0")
    void validate_withOneSamplingRate_shouldPassValidation() {
        // Given: OpenTelemetryProperties with samplingRate 1.0
        properties.setSamplingRate(1.0);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when samplingRate is 0.5")
    void validate_withHalfSamplingRate_shouldPassValidation() {
        // Given: OpenTelemetryProperties with samplingRate 0.5
        properties.setSamplingRate(0.5);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should set and get disabled property")
    void setDisabled_withValidValue_shouldSetAndGet() {
        // Given: A boolean value

        // When: Setting the disabled property to true
        properties.setDisabled(true);

        // Then: Disabled property is set correctly
        assertThat(properties.isDisabled()).isTrue();
    }

    @Test
    @DisplayName("Should set and get url property")
    void setUrl_withValidValue_shouldSetAndGet() {
        // Given: A valid url value
        final String url = "http://jaeger:4318";

        // When: Setting the url property
        properties.setUrl(url);

        // Then: URL property is set correctly
        assertThat(properties.getUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("Should set and get timeout property")
    void setTimeout_withValidValue_shouldSetAndGet() {
        // Given: A valid timeout value
        final String timeout = "30s";

        // When: Setting the timeout property
        properties.setTimeout(timeout);

        // Then: Timeout property is set correctly
        assertThat(properties.getTimeout()).isEqualTo(timeout);
    }

    @Test
    @DisplayName("Should set and get exporter property")
    void setExporter_withValidValue_shouldSetAndGet() {
        // Given: A valid exporter value
        final String exporter = "jaeger";

        // When: Setting the exporter property
        properties.setExporter(exporter);

        // Then: Exporter property is set correctly
        assertThat(properties.getExporter()).isEqualTo(exporter);
    }

    @Test
    @DisplayName("Should set and get samplingRate property")
    void setSamplingRate_withValidValue_shouldSetAndGet() {
        // Given: A valid samplingRate value
        final double samplingRate = 0.75;

        // When: Setting the samplingRate property
        properties.setSamplingRate(samplingRate);

        // Then: SamplingRate property is set correctly
        assertThat(properties.getSamplingRate()).isEqualTo(samplingRate);
    }

    @Test
    @DisplayName("Should set and get excludedMethods property")
    void setExcludedMethods_withValidValue_shouldSetAndGet() {
        // Given: A set of method names to exclude
        final Set<String> excludedMethods = Set.of("metrics", "info", "debug");

        // When: Setting the excludedMethods property
        properties.setExcludedMethods(excludedMethods);

        // Then: ExcludedMethods property is set correctly
        assertThat(properties.getExcludedMethods()).isEqualTo(excludedMethods);
    }

    @Test
    @DisplayName("Should fail validation when multiple fields are invalid")
    void validate_withMultipleInvalidFields_shouldFailValidationForEach() {
        // Given: OpenTelemetryProperties with multiple invalid fields
        properties.setUrl(null);
        properties.setTimeout(null);
        properties.setExporter(null);
        properties.setSamplingRate(-1.0);

        // When: Validating the properties
        final Set<ConstraintViolation<OpenTelemetryProperties>> violations = validator.validate(properties);

        // Then: Validation fails for all invalid fields
        assertThat(violations).hasSize(4);
    }
}
