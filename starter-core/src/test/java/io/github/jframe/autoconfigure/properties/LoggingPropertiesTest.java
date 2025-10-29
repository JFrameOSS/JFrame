package io.github.jframe.autoconfigure.properties;

import io.github.jframe.logging.model.PathDefinition;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import jakarta.validation.ConstraintViolation;
import jakarta.validation.Validation;
import jakarta.validation.Validator;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.http.MediaType;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link LoggingProperties}.
 *
 * <p>Verifies the LoggingProperties functionality including:
 * <ul>
 * <li>Validation constraints for required fields</li>
 * <li>Default values for all properties</li>
 * <li>Property getters and setters</li>
 * <li>Min value constraint for responseLength</li>
 * </ul>
 */
@DisplayName("Properties - LoggingProperties")
class LoggingPropertiesTest extends UnitTest {

    private Validator validator;
    private LoggingProperties properties;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        properties = new LoggingProperties();
    }

    @Test
    @DisplayName("Should pass validation with default values")
    void validate_withDefaults_shouldPassValidation() {
        // Given: LoggingProperties with default values

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should have default disabled value of false")
    void isDisabled_withDefaults_shouldReturnFalse() {
        // Given: LoggingProperties with default values

        // When: Getting disabled value
        final boolean disabled = properties.isDisabled();

        // Then: Default value is false
        assertThat(disabled).isFalse();
    }

    @Test
    @DisplayName("Should have default responseLength value of -1")
    void getResponseLength_withDefaults_shouldReturnMinusOne() {
        // Given: LoggingProperties with default values

        // When: Getting responseLength value
        final int responseLength = properties.getResponseLength();

        // Then: Default value is -1 (unlimited)
        assertThat(responseLength).isEqualTo(-1);
    }

    @Test
    @DisplayName("Should have default bodyExcludedContentTypes including multipart/form-data")
    void getBodyExcludedContentTypes_withDefaults_shouldIncludeMultipartFormData() {
        // Given: LoggingProperties with default values

        // When: Getting bodyExcludedContentTypes value
        final List<MediaType> excludedTypes = properties.getBodyExcludedContentTypes();

        // Then: Default list includes multipart/form-data
        assertThat(excludedTypes).contains(MediaType.MULTIPART_FORM_DATA);
    }

    @Test
    @DisplayName("Should have default excludePaths including /actuator/*")
    void getExcludePaths_withDefaults_shouldIncludeActuatorPath() {
        // Given: LoggingProperties with default values

        // When: Getting excludePaths value
        final List<PathDefinition> excludePaths = properties.getExcludePaths();

        // Then: Default list includes /actuator/* pattern
        assertThat(excludePaths).hasSize(1);
        assertThat(excludePaths.get(0).getPattern()).isEqualTo("/actuator/*");
    }

    @Test
    @DisplayName("Should have default fieldsToMask including common sensitive fields")
    void getFieldsToMask_withDefaults_shouldIncludeCommonSensitiveFields() {
        // Given: LoggingProperties with default values

        // When: Getting fieldsToMask value
        final List<String> fieldsToMask = properties.getFieldsToMask();

        // Then: Default list includes password, client_secret, secret, and keyPassphrase
        assertThat(fieldsToMask)
            .contains("password", "keyPassphrase", "client_secret", "secret");
    }

    @Test
    @DisplayName("Should have default allowedContentTypes including JSON and common types")
    void getAllowedContentTypes_withDefaults_shouldIncludeCommonTypes() {
        // Given: LoggingProperties with default values

        // When: Getting allowedContentTypes value
        final List<MediaType> allowedTypes = properties.getAllowedContentTypes();

        // Then: Default list includes JSON, XML, plain text, and other common types
        assertThat(allowedTypes)
            .contains(
                MediaType.parseMediaType("application/json"),
                MediaType.parseMediaType("application/xml"),
                MediaType.parseMediaType("text/plain")
            );
        assertThat(allowedTypes).hasSizeGreaterThan(5);
    }

    @Test
    @DisplayName("Should fail validation when responseLength is less than -1")
    void validate_withResponseLengthLessThanMinusOne_shouldFailValidation() {
        // Given: LoggingProperties with responseLength less than -1
        properties.setResponseLength(-2);

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about minimum value
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Response length must be -1 (unlimited) or a positive number");
    }

    @Test
    @DisplayName("Should pass validation when responseLength is -1")
    void validate_withResponseLengthMinusOne_shouldPassValidation() {
        // Given: LoggingProperties with responseLength -1 (unlimited)
        properties.setResponseLength(-1);

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when responseLength is positive")
    void validate_withPositiveResponseLength_shouldPassValidation() {
        // Given: LoggingProperties with positive responseLength
        properties.setResponseLength(2000);

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should pass validation when responseLength is zero")
    void validate_withZeroResponseLength_shouldPassValidation() {
        // Given: LoggingProperties with zero responseLength
        properties.setResponseLength(0);

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when allowedContentTypes is empty")
    void validate_withEmptyAllowedContentTypes_shouldFailValidation() {
        // Given: LoggingProperties with empty allowedContentTypes
        properties.setAllowedContentTypes(new ArrayList<>());

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about empty list
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage())
            .contains("Allowed content types list cannot be empty");
    }

    @Test
    @DisplayName("Should pass validation when allowedContentTypes has one element")
    void validate_withSingleAllowedContentType_shouldPassValidation() {
        // Given: LoggingProperties with single allowedContentType
        properties.setAllowedContentTypes(Collections.singletonList(MediaType.APPLICATION_JSON));

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

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
    @DisplayName("Should set and get responseLength property")
    void setResponseLength_withValidValue_shouldSetAndGet() {
        // Given: A valid responseLength value
        final int responseLength = 5000;

        // When: Setting the responseLength property
        properties.setResponseLength(responseLength);

        // Then: ResponseLength property is set correctly
        assertThat(properties.getResponseLength()).isEqualTo(responseLength);
    }

    @Test
    @DisplayName("Should set and get bodyExcludedContentTypes property")
    void setBodyExcludedContentTypes_withValidValue_shouldSetAndGet() {
        // Given: A list of media types to exclude
        final List<MediaType> excludedTypes = List.of(
            MediaType.MULTIPART_FORM_DATA,
            MediaType.APPLICATION_OCTET_STREAM
        );

        // When: Setting the bodyExcludedContentTypes property
        properties.setBodyExcludedContentTypes(excludedTypes);

        // Then: BodyExcludedContentTypes property is set correctly
        assertThat(properties.getBodyExcludedContentTypes()).isEqualTo(excludedTypes);
    }

    @Test
    @DisplayName("Should set and get excludePaths property")
    void setExcludePaths_withValidValue_shouldSetAndGet() {
        // Given: A list of path definitions to exclude
        final List<PathDefinition> excludePaths = List.of(
            new PathDefinition("/health"),
            new PathDefinition("GET", "/metrics")
        );

        // When: Setting the excludePaths property
        properties.setExcludePaths(excludePaths);

        // Then: ExcludePaths property is set correctly
        assertThat(properties.getExcludePaths()).isEqualTo(excludePaths);
    }

    @Test
    @DisplayName("Should set and get fieldsToMask property")
    void setFieldsToMask_withValidValue_shouldSetAndGet() {
        // Given: A list of field names to mask
        final List<String> fieldsToMask = List.of("password", "token", "apiKey");

        // When: Setting the fieldsToMask property
        properties.setFieldsToMask(fieldsToMask);

        // Then: FieldsToMask property is set correctly
        assertThat(properties.getFieldsToMask()).isEqualTo(fieldsToMask);
    }

    @Test
    @DisplayName("Should set and get allowedContentTypes property")
    void setAllowedContentTypes_withValidValue_shouldSetAndGet() {
        // Given: A list of allowed content types
        final List<MediaType> allowedTypes = List.of(
            MediaType.APPLICATION_JSON,
            MediaType.TEXT_PLAIN
        );

        // When: Setting the allowedContentTypes property
        properties.setAllowedContentTypes(allowedTypes);

        // Then: AllowedContentTypes property is set correctly
        assertThat(properties.getAllowedContentTypes()).isEqualTo(allowedTypes);
    }

    @Test
    @DisplayName("Should have correct configuration prefix constant")
    void configPrefix_shouldHaveCorrectValue() {
        // Given/When: Getting the CONFIG_PREFIX constant

        // Then: Prefix matches expected value
        assertThat(LoggingProperties.CONFIG_PREFIX).isEqualTo("jframe.logging");
    }

    @Test
    @DisplayName("Should allow null lists for optional properties")
    void validate_withNullOptionalLists_shouldPassValidation() {
        // Given: LoggingProperties with null optional lists
        properties.setBodyExcludedContentTypes(null);
        properties.setExcludePaths(null);
        properties.setFieldsToMask(null);

        // When: Validating the properties
        final Set<ConstraintViolation<LoggingProperties>> violations = validator.validate(properties);

        // Then: No validation violations for optional lists
        assertThat(violations).isEmpty();
    }
}
