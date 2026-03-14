package io.github.jframe.logging;

import io.github.support.UnitTest;
import io.smallrye.config.ConfigMapping;
import io.smallrye.config.WithDefault;

import java.lang.reflect.Method;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Tests for {@link LoggingConfig} — Quarkus {@code @ConfigMapping} interface.
 *
 * <p>Verifies via reflection that:
 * <ul>
 * <li>The interface carries the correct {@code @ConfigMapping} annotation and prefix</li>
 * <li>Each method exists, returns the correct type, and is annotated with {@code @WithDefault}
 * carrying the expected default value</li>
 * </ul>
 *
 * <p>These tests are intentionally annotation/contract-only; SmallRye Config runtime binding
 * is outside the scope of unit tests and is covered by Quarkus integration tests.
 */
@DisplayName("Quarkus Core - Logging Config")
public class LoggingConfigTest extends UnitTest {

    // -------------------------------------------------------------------------
    // @ConfigMapping annotation — interface-level contract
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should be annotated with @ConfigMapping")
    public void shouldBeAnnotatedWithConfigMapping() {
        // Given: The LoggingConfig interface class

        // When: Retrieving the @ConfigMapping annotation
        final ConfigMapping annotation = LoggingConfig.class.getAnnotation(ConfigMapping.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @ConfigMapping prefix 'jframe.logging'")
    public void shouldHaveConfigMappingPrefixJframeLogging() {
        // Given: The @ConfigMapping annotation on the interface

        // When: Reading the prefix attribute
        final String prefix = LoggingConfig.class.getAnnotation(ConfigMapping.class).prefix();

        // Then: Prefix is 'jframe.logging'
        assertThat(prefix, is("jframe.logging"));
    }

    @Test
    @DisplayName("Should be an interface")
    public void shouldBeAnInterface() {
        // Given: The LoggingConfig class object

        // When: Checking if it is an interface
        final boolean isInterface = LoggingConfig.class.isInterface();

        // Then: It must be an interface
        assertThat(isInterface, is(true));
    }

    // -------------------------------------------------------------------------
    // disabled() — boolean, default false
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'disabled' method returning boolean")
    public void shouldDeclareDisabledMethodReturningBoolean() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'disabled' method
        final Method method = LoggingConfig.class.getMethod("disabled");

        // Then: Return type must be boolean
        assertThat(method.getReturnType(), is(equalTo(boolean.class)));
    }

    @Test
    @DisplayName("Should annotate 'disabled' with @WithDefault")
    public void shouldAnnotateDisabledWithWithDefault() throws NoSuchMethodException {
        // Given: The 'disabled' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("disabled").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault('false') on 'disabled'")
    public void shouldHaveWithDefaultFalseOnDisabled() throws NoSuchMethodException {
        // Given: The 'disabled' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("disabled").getAnnotation(WithDefault.class).value();

        // Then: Default is 'false'
        assertThat(defaultValue, is("false"));
    }

    // -------------------------------------------------------------------------
    // responseLength() — int, default -1
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'responseLength' method returning int")
    public void shouldDeclareResponseLengthMethodReturningInt() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'responseLength' method
        final Method method = LoggingConfig.class.getMethod("responseLength");

        // Then: Return type must be int
        assertThat(method.getReturnType(), is(equalTo(int.class)));
    }

    @Test
    @DisplayName("Should annotate 'responseLength' with @WithDefault")
    public void shouldAnnotateResponseLengthWithWithDefault() throws NoSuchMethodException {
        // Given: The 'responseLength' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("responseLength").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault('-1') on 'responseLength'")
    public void shouldHaveWithDefaultMinusOneOnResponseLength() throws NoSuchMethodException {
        // Given: The 'responseLength' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("responseLength").getAnnotation(WithDefault.class).value();

        // Then: Default is '-1'
        assertThat(defaultValue, is("-1"));
    }

    // -------------------------------------------------------------------------
    // bodyExcludedContentTypes() — List<String>, default ["multipart/form-data"]
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'bodyExcludedContentTypes' method returning List")
    public void shouldDeclareBodyExcludedContentTypesMethodReturningList() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'bodyExcludedContentTypes' method
        final Method method = LoggingConfig.class.getMethod("bodyExcludedContentTypes");

        // Then: Return type must be List
        assertThat(method.getReturnType(), is(equalTo(List.class)));
    }

    @Test
    @DisplayName("Should annotate 'bodyExcludedContentTypes' with @WithDefault")
    public void shouldAnnotateBodyExcludedContentTypesWithWithDefault() throws NoSuchMethodException {
        // Given: The 'bodyExcludedContentTypes' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("bodyExcludedContentTypes").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'multipart/form-data' on 'bodyExcludedContentTypes'")
    public void shouldHaveWithDefaultMultipartFormDataOnBodyExcludedContentTypes() throws NoSuchMethodException {
        // Given: The 'bodyExcludedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("bodyExcludedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'multipart/form-data'
        assertThat(defaultValue, containsString("multipart/form-data"));
    }

    // -------------------------------------------------------------------------
    // excludePaths() — List<String>, default ["/actuator/*"]
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'excludePaths' method returning List")
    public void shouldDeclareExcludePathsMethodReturningList() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'excludePaths' method
        final Method method = LoggingConfig.class.getMethod("excludePaths");

        // Then: Return type must be List
        assertThat(method.getReturnType(), is(equalTo(List.class)));
    }

    @Test
    @DisplayName("Should annotate 'excludePaths' with @WithDefault")
    public void shouldAnnotateExcludePathsWithWithDefault() throws NoSuchMethodException {
        // Given: The 'excludePaths' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("excludePaths").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault('/actuator/*' on 'excludePaths'")
    public void shouldHaveWithDefaultActuatorWildcardOnExcludePaths() throws NoSuchMethodException {
        // Given: The 'excludePaths' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("excludePaths").getAnnotation(WithDefault.class).value();

        // Then: Default contains '/actuator/*'
        assertThat(defaultValue, containsString("/actuator/*"));
    }

    // -------------------------------------------------------------------------
    // fieldsToMask() — List<String>, default ["password","keyPassphrase","client_secret","secret"]
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'fieldsToMask' method returning List")
    public void shouldDeclareFieldsToMaskMethodReturningList() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'fieldsToMask' method
        final Method method = LoggingConfig.class.getMethod("fieldsToMask");

        // Then: Return type must be List
        assertThat(method.getReturnType(), is(equalTo(List.class)));
    }

    @Test
    @DisplayName("Should annotate 'fieldsToMask' with @WithDefault")
    public void shouldAnnotateFieldsToMaskWithWithDefault() throws NoSuchMethodException {
        // Given: The 'fieldsToMask' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("fieldsToMask").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'password' on 'fieldsToMask'")
    public void shouldHaveWithDefaultContainingPasswordOnFieldsToMask() throws NoSuchMethodException {
        // Given: The 'fieldsToMask' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("fieldsToMask").getAnnotation(WithDefault.class).value();

        // Then: Default contains 'password'
        assertThat(defaultValue, containsString("password"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'keyPassphrase' on 'fieldsToMask'")
    public void shouldHaveWithDefaultContainingKeyPassphraseOnFieldsToMask() throws NoSuchMethodException {
        // Given: The 'fieldsToMask' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("fieldsToMask").getAnnotation(WithDefault.class).value();

        // Then: Default contains 'keyPassphrase'
        assertThat(defaultValue, containsString("keyPassphrase"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'client_secret' on 'fieldsToMask'")
    public void shouldHaveWithDefaultContainingClientSecretOnFieldsToMask() throws NoSuchMethodException {
        // Given: The 'fieldsToMask' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("fieldsToMask").getAnnotation(WithDefault.class).value();

        // Then: Default contains 'client_secret'
        assertThat(defaultValue, containsString("client_secret"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'secret' on 'fieldsToMask'")
    public void shouldHaveWithDefaultContainingSecretOnFieldsToMask() throws NoSuchMethodException {
        // Given: The 'fieldsToMask' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("fieldsToMask").getAnnotation(WithDefault.class).value();

        // Then: Default contains 'secret'
        assertThat(defaultValue, containsString("secret"));
    }

    // -------------------------------------------------------------------------
    // allowedContentTypes() — List<String>, default 12 media type strings
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare 'allowedContentTypes' method returning List")
    public void shouldDeclareAllowedContentTypesMethodReturningList() throws NoSuchMethodException {
        // Given: The LoggingConfig interface

        // When: Looking up the 'allowedContentTypes' method
        final Method method = LoggingConfig.class.getMethod("allowedContentTypes");

        // Then: Return type must be List
        assertThat(method.getReturnType(), is(equalTo(List.class)));
    }

    @Test
    @DisplayName("Should annotate 'allowedContentTypes' with @WithDefault")
    public void shouldAnnotateAllowedContentTypesWithWithDefault() throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Retrieving the @WithDefault annotation
        final WithDefault annotation =
            LoggingConfig.class.getMethod("allowedContentTypes").getAnnotation(WithDefault.class);

        // Then: Annotation must be present
        assertThat(annotation, is(notNullValue()));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'application/json' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingApplicationJsonOnAllowedContentTypes() throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'application/json'
        assertThat(defaultValue, containsString("application/json"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'application/xml' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingApplicationXmlOnAllowedContentTypes() throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'application/xml'
        assertThat(defaultValue, containsString("application/xml"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'text/plain' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingTextPlainOnAllowedContentTypes() throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'text/plain'
        assertThat(defaultValue, containsString("text/plain"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'application/problem+json' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingApplicationProblemJsonOnAllowedContentTypes()
        throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'application/problem+json'
        assertThat(defaultValue, containsString("application/problem+json"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'application/graphql+json' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingApplicationGraphqlJsonOnAllowedContentTypes()
        throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'application/graphql+json'
        assertThat(defaultValue, containsString("application/graphql+json"));
    }

    @Test
    @DisplayName("Should have @WithDefault containing 'multipart/form-data' on 'allowedContentTypes'")
    public void shouldHaveWithDefaultContainingMultipartFormDataOnAllowedContentTypes()
        throws NoSuchMethodException {
        // Given: The 'allowedContentTypes' method on LoggingConfig

        // When: Reading the default value
        final String defaultValue =
            LoggingConfig.class.getMethod("allowedContentTypes")
                .getAnnotation(WithDefault.class).value();

        // Then: Default contains 'multipart/form-data'
        assertThat(defaultValue, containsString("multipart/form-data"));
    }

    // -------------------------------------------------------------------------
    // Interface method count — ensures no accidental extra/missing methods
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should declare exactly 6 methods")
    public void shouldDeclareExactlySixMethods() {
        // Given: The LoggingConfig interface

        // When: Counting declared methods
        final int methodCount = LoggingConfig.class.getDeclaredMethods().length;

        // Then: Exactly 6 methods are declared
        assertThat(methodCount, is(6));
    }

    // -------------------------------------------------------------------------
    // Helper — avoids Hamcrest ambiguity with Class<?> equals
    // -------------------------------------------------------------------------

    private static <T> org.hamcrest.Matcher<T> equalTo(T operand) {
        return org.hamcrest.Matchers.equalTo(operand);
    }
}
