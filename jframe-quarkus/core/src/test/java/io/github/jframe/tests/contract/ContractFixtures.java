package io.github.jframe.tests.contract;

import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;
import java.util.Map;

/**
 * Provides shared test fixture data loaded from classpath JSON files for use in
 * cross-framework contract tests between Spring and Quarkus adapters.
 *
 * <p>Fixture files reside under {@code src/main/resources/fixtures/} and are
 * loaded at test time from the classpath. All load methods are static so
 * callers need no instance.
 */
public final class ContractFixtures {

    /** Path prefix for fixture JSON resources. */
    private static final String FIXTURES_PATH = "fixtures/";

    /** Shared Jackson mapper used for all fixture deserialization. */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /** Private constructor — utility class, not meant to be instantiated. */
    private ContractFixtures() {
    }

    // ============================================================
    // Public fixture-loading methods
    // ============================================================

    /**
     * Loads validation-input fixture scenarios from {@code fixtures/validation-input.json}.
     *
     * <p>Each scenario describes a set of input fields and the validation errors
     * that are expected when those fields are submitted.
     *
     * @return an unmodifiable list of {@link ValidationInputScenario} records
     * @throws UncheckedIOException if the resource cannot be read
     */
    public static List<ValidationInputScenario> loadValidationInputs() {
        return loadFixture("validation-input.json", new TypeReference<>() {
        });
    }

    /**
     * Loads search-criteria fixture scenarios from {@code fixtures/search-criteria.json}.
     *
     * <p>Each scenario describes a search criterium (field name, type, value) together
     * with a human-readable description of the expected search behaviour.
     *
     * @return an unmodifiable list of {@link SearchCriteriaScenario} records
     * @throws UncheckedIOException if the resource cannot be read
     */
    public static List<SearchCriteriaScenario> loadSearchCriteria() {
        return loadFixture("search-criteria.json", new TypeReference<>() {
        });
    }

    /**
     * Loads exception-scenario fixtures from {@code fixtures/exception-scenarios.json}.
     *
     * <p>Each scenario identifies an exception type, a message, the expected HTTP status
     * code, and the expected error message that should appear in the response.
     *
     * @return an unmodifiable list of {@link ExceptionScenario} records
     * @throws UncheckedIOException if the resource cannot be read
     */
    public static List<ExceptionScenario> loadExceptionScenarios() {
        return loadFixture("exception-scenarios.json", new TypeReference<>() {
        });
    }

    // ============================================================
    // Internal helpers
    // ============================================================

    /**
     * Loads a fixture file from the classpath and deserialises it using the supplied type reference.
     *
     * @param <T>      the target type
     * @param fileName the file name relative to the {@code fixtures/} resource directory
     * @param typeRef  Jackson type reference describing the target type
     * @return the deserialised value
     * @throws UncheckedIOException if the resource cannot be found or read
     */
    private static <T> T loadFixture(final String fileName, final TypeReference<T> typeRef) {
        final String resourcePath = FIXTURES_PATH + fileName;
        try (InputStream stream = ContractFixtures.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new UncheckedIOException(
                    "Fixture resource not found on classpath: " + resourcePath,
                    new IOException(resourcePath)
                );
            }
            return MAPPER.readValue(stream, typeRef);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to load fixture: " + resourcePath, exception);
        }
    }

    // ============================================================
    // Fixture model classes
    // ============================================================

    /** A single validation-input test scenario. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationInputScenario {

        private String name;
        private Map<String, Object> input;
        private List<ValidationErrorDescriptor> expectedErrors;
    }


    /** A single expected validation error. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ValidationErrorDescriptor {

        private String field;
        private String code;
    }


    /** A single search-criteria test scenario. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchCriteriaScenario {

        private String name;
        private SearchCriteriumDescriptor criteria;
        private String expectedDescription;
    }


    /** A search criterium used inside a {@link SearchCriteriaScenario}. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class SearchCriteriumDescriptor {

        private String fieldName;
        private String fieldType;
        private String value;
    }


    /** A single exception-scenario fixture entry. */
    @lombok.Data
    @lombok.NoArgsConstructor
    @lombok.AllArgsConstructor
    public static class ExceptionScenario {

        private String name;
        private String exceptionType;
        private String endpoint;
        private String message;
        private int expectedStatusCode;
        private String expectedErrorMessage;
    }
}
