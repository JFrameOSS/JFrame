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
    // Fixture model records
    // ============================================================

    /**
     * Represents a single validation-input test scenario.
     *
     * @param name           unique scenario identifier
     * @param input          raw input fields as a generic map (field name → raw value)
     * @param expectedErrors list of validation error descriptors that should be produced
     */
    public record ValidationInputScenario(
        String name,
        Map<String, Object> input,
        List<ValidationErrorDescriptor> expectedErrors) {
    }


    /**
     * Describes a single expected validation error loaded from a fixture file.
     *
     * <p>Uses a record so that Jackson can deserialise it without annotations.
     * Consumers may compare instances against {@code io.github.jframe.validation.ValidationError}
     * by comparing the {@code field} and {@code code} fields directly.
     *
     * @param field the field that failed validation, may be {@code null} for object-level errors
     * @param code  the validation error code (e.g. {@code "REQUIRED"})
     */
    public record ValidationErrorDescriptor(String field, String code) {
    }


    /**
     * Represents a single search-criteria test scenario.
     *
     * @param name                scenario identifier
     * @param criteria            the search criterium descriptor
     * @param expectedDescription human-readable description of the expected search behaviour
     */
    public record SearchCriteriaScenario(
        String name,
        SearchCriteriumDescriptor criteria,
        String expectedDescription) {
    }


    /**
     * Describes a search criterium used inside a {@link SearchCriteriaScenario}.
     *
     * @param fieldName name of the field being searched
     * @param fieldType type of the field (e.g. {@code TEXT}, {@code ENUM})
     * @param value     the search value
     */
    public record SearchCriteriumDescriptor(
        String fieldName,
        String fieldType,
        String value) {
    }


    /**
     * Represents a single exception-scenario fixture entry.
     *
     * @param name                 unique scenario identifier
     * @param exceptionType        simple class name of the exception (e.g. {@code BadRequestException})
     * @param endpoint             relative URL path used by HTTP-based test clients (e.g. {@code /bad-request})
     * @param message              the message passed to the exception constructor
     * @param expectedStatusCode   HTTP status code expected in the error response
     * @param expectedErrorMessage error message expected in the error response body
     */
    public record ExceptionScenario(
        String name,
        String exceptionType,
        String endpoint,
        String message,
        int expectedStatusCode,
        String expectedErrorMessage) {
    }
}
