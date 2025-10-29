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
 * Unit tests for {@link ApplicationProperties}.
 *
 * <p>Verifies the ApplicationProperties functionality including:
 * <ul>
 * <li>Validation constraints for required fields</li>
 * <li>Default values</li>
 * <li>Property getters and setters</li>
 * </ul>
 */
@DisplayName("Properties - ApplicationProperties")
class ApplicationPropertiesTest extends UnitTest {

    private Validator validator;
    private ApplicationProperties properties;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        validator = Validation.buildDefaultValidatorFactory().getValidator();
        properties = new ApplicationProperties();
    }

    @Test
    @DisplayName("Should pass validation with all required fields set")
    void validate_withAllRequiredFields_shouldPassValidation() {
        // Given: ApplicationProperties with all required fields set
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: No validation violations
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should fail validation when name is null")
    void validate_withNullName_shouldFailValidation() {
        // Given: ApplicationProperties with null name
        properties.setName(null);
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank name
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Application name must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when name is blank")
    void validate_withBlankName_shouldFailValidation() {
        // Given: ApplicationProperties with blank name
        properties.setName("   ");
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank name
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Application name must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when group is null")
    void validate_withNullGroup_shouldFailValidation() {
        // Given: ApplicationProperties with null group
        properties.setName("test-service");
        properties.setGroup(null);
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank group
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("group/namespace must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when group is blank")
    void validate_withBlankGroup_shouldFailValidation() {
        // Given: ApplicationProperties with blank group
        properties.setName("test-service");
        properties.setGroup("   ");
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank group
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("group/namespace must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when version is null")
    void validate_withNullVersion_shouldFailValidation() {
        // Given: ApplicationProperties with null version
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion(null);
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank version
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("version must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when version is blank")
    void validate_withBlankVersion_shouldFailValidation() {
        // Given: ApplicationProperties with blank version
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion("   ");
        properties.setEnvironment("dev");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank version
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("version must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when environment is null")
    void validate_withNullEnvironment_shouldFailValidation() {
        // Given: ApplicationProperties with null environment
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment(null);

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank environment
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Application environment must not be blank");
    }

    @Test
    @DisplayName("Should fail validation when environment is blank")
    void validate_withBlankEnvironment_shouldFailValidation() {
        // Given: ApplicationProperties with blank environment
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment("   ");

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails with message about blank environment
        assertThat(violations).hasSize(1);
        assertThat(violations.iterator().next().getMessage()).contains("Application environment must not be blank");
    }

    @Test
    @DisplayName("Should have default environment value of 'dev'")
    void getEnvironment_withDefaults_shouldReturnDev() {
        // Given: ApplicationProperties with default values

        // When: Getting environment value
        final String environment = properties.getEnvironment();

        // Then: Default value is 'dev'
        assertThat(environment).isEqualTo("dev");
    }

    @Test
    @DisplayName("Should allow null url value")
    void validate_withNullUrl_shouldPassValidation() {
        // Given: ApplicationProperties with null url (url is optional)
        properties.setName("test-service");
        properties.setGroup("test-group");
        properties.setVersion("1.0.0");
        properties.setEnvironment("dev");
        properties.setUrl(null);

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: No validation violations since url is optional
        assertThat(violations).isEmpty();
    }

    @Test
    @DisplayName("Should set and get name property")
    void setName_withValidValue_shouldSetAndGet() {
        // Given: A valid name value
        final String name = "my-service";

        // When: Setting the name property
        properties.setName(name);

        // Then: Name property is set correctly
        assertThat(properties.getName()).isEqualTo(name);
    }

    @Test
    @DisplayName("Should set and get group property")
    void setGroup_withValidValue_shouldSetAndGet() {
        // Given: A valid group value
        final String group = "my-group";

        // When: Setting the group property
        properties.setGroup(group);

        // Then: Group property is set correctly
        assertThat(properties.getGroup()).isEqualTo(group);
    }

    @Test
    @DisplayName("Should set and get version property")
    void setVersion_withValidValue_shouldSetAndGet() {
        // Given: A valid version value
        final String version = "2.0.0";

        // When: Setting the version property
        properties.setVersion(version);

        // Then: Version property is set correctly
        assertThat(properties.getVersion()).isEqualTo(version);
    }

    @Test
    @DisplayName("Should set and get environment property")
    void setEnvironment_withValidValue_shouldSetAndGet() {
        // Given: A valid environment value
        final String environment = "prod";

        // When: Setting the environment property
        properties.setEnvironment(environment);

        // Then: Environment property is set correctly
        assertThat(properties.getEnvironment()).isEqualTo(environment);
    }

    @Test
    @DisplayName("Should set and get url property")
    void setUrl_withValidValue_shouldSetAndGet() {
        // Given: A valid url value
        final String url = "https://example.com:8080";

        // When: Setting the url property
        properties.setUrl(url);

        // Then: URL property is set correctly
        assertThat(properties.getUrl()).isEqualTo(url);
    }

    @Test
    @DisplayName("Should fail validation when multiple fields are invalid")
    void validate_withMultipleInvalidFields_shouldFailValidationForEach() {
        // Given: ApplicationProperties with multiple null fields
        properties.setName(null);
        properties.setGroup(null);
        properties.setVersion(null);
        properties.setEnvironment(null);

        // When: Validating the properties
        final Set<ConstraintViolation<ApplicationProperties>> violations = validator.validate(properties);

        // Then: Validation fails for all null required fields
        assertThat(violations).hasSize(4);
    }
}
