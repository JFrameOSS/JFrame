package io.github.jframe.tests.contract;

import io.github.jframe.validation.ValidationError;
import tools.jackson.databind.JsonNode;
import tools.jackson.databind.ObjectMapper;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.util.List;

import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;

/**
 * Provides assertion utilities for cross-framework contract verification between
 * Spring and Quarkus adapter implementations.
 *
 * <p>Both frameworks handle exceptions differently:
 * <ul>
 * <li>Spring wraps error information in an {@code ErrorResponseResource} that carries
 * HTTP metadata alongside the domain error fields.</li>
 * <li>Quarkus returns raw domain objects directly.</li>
 * </ul>
 *
 * <p>All methods in this class use reflection-based introspection so that the same
 * assertions can be applied to either framework's response without introducing a
 * compile-time dependency on either adapter module.
 *
 * <p>Assertions use Hamcrest matchers, consistent with the project-wide testing standard.
 */
public final class ContractVerifier {

    /** Path prefix for golden JSON resources on the classpath. */
    private static final String GOLDEN_PATH = "golden/";

    /** Shared Jackson mapper used for all JSON operations. */
    private static final ObjectMapper MAPPER = JsonMapper.builder().build();

    /** Suffix used in "could not resolve field" assertion error messages. */
    private static final String USING_METHODS_SUFFIX = "' using methods: ";

    /** Delimiter used when joining method name candidates in error messages. */
    private static final String METHOD_NAMES_DELIMITER = ", ";

    /** Getter name for the error code field. */
    private static final String GET_CODE = "getCode";

    /** Private constructor — utility class, not meant to be instantiated. */
    private ContractVerifier() {
    }

    // ============================================================
    // Golden-file assertions
    // ============================================================

    /**
     * Asserts that the serialised form of {@code actual} matches the JSON stored in
     * the named golden file under {@code golden/}.
     *
     * <p>Comparison is performed as a JSON-tree equality check so that differences in
     * field ordering do not cause spurious failures.
     *
     * @param goldenFile file name relative to the {@code golden/} resource directory
     * @param actual     the object whose serialised JSON representation is compared
     * @throws UncheckedIOException if the golden file cannot be read
     * @throws AssertionError       if the actual JSON does not match the golden JSON
     */
    public static void assertMatchesGolden(final String goldenFile, final Object actual) {
        final JsonNode goldenNode = loadGoldenNode(goldenFile);
        final JsonNode actualNode = toJsonNode(actual);
        MatcherAssert.assertThat(
            "JSON output for golden file '" + goldenFile + "' does not match",
            actualNode,
            Matchers.is(Matchers.equalTo(goldenNode))
        );
    }

    // ============================================================
    // Error-response assertions
    // ============================================================

    /**
     * Asserts that an error response object (from either Spring or Quarkus) carries the
     * expected HTTP status code and error code.
     *
     * <p>The method inspects the response object via reflection, probing for common getter
     * names used across both frameworks:
     * <ul>
     * <li>Status: {@code getStatus()}, {@code getStatusCode()}, {@code status}</li>
     * <li>Error code: {@code getErrorCode()}, {@code getCode()}, {@code errorCode}</li>
     * </ul>
     *
     * @param expectedStatus    expected HTTP status code (e.g. {@code 400})
     * @param expectedErrorCode expected error code string (e.g. {@code "VALIDATION_ERROR"})
     * @param actualResponse    the response object returned by either Spring or Quarkus
     * @throws AssertionError if either field is missing or does not match
     */
    public static void assertErrorResponse(
        final int expectedStatus,
        final String expectedErrorCode,
        final Object actualResponse) {

        final int actualStatus = resolveIntField(actualResponse, "getStatus", "getStatusCode");
        final String actualErrorCode = resolveStringField(actualResponse, "getErrorCode", GET_CODE);

        MatcherAssert.assertThat(
            "HTTP status code mismatch",
            actualStatus,
            Matchers.is(Matchers.equalTo(expectedStatus))
        );

        MatcherAssert.assertThat(
            "Error code mismatch",
            actualErrorCode,
            Matchers.is(Matchers.equalTo(expectedErrorCode))
        );
    }

    // ============================================================
    // Validation-error assertions
    // ============================================================

    /**
     * Asserts that an error response object contains a set of validation errors that
     * exactly match the supplied expected list (order-insensitive).
     *
     * <p>The method inspects the response object via reflection, probing for common getter
     * names that expose validation errors across both frameworks:
     * {@code getErrors()}, {@code getValidationErrors()}, {@code getViolations()}.
     *
     * @param expected       the expected list of {@link ValidationError} instances
     * @param actualResponse the response object returned by either Spring or Quarkus
     * @throws AssertionError if the actual validation errors do not match the expected ones
     */
    public static void assertValidationErrors(
        final List<ValidationError> expected,
        final Object actualResponse) {

        final List<?> actualErrors = resolveListField(
            actualResponse,
            "getErrors",
            "getValidationErrors",
            "getViolations"
        );

        MatcherAssert.assertThat(
            "Validation error count mismatch",
            actualErrors.size(),
            Matchers.is(Matchers.equalTo(expected.size()))
        );

        for (final ValidationError expectedError : expected) {
            final boolean found = actualErrors.stream().anyMatch(item -> {
                final String actualField = resolveStringFieldSilently(item, "getField");
                final String actualCode = resolveStringFieldSilently(item, GET_CODE);
                return expectedError.getField() != null
                    ? expectedError.getField().equals(actualField) && expectedError.getCode().equals(actualCode)
                    : expectedError.getCode().equals(actualCode);
            });

            MatcherAssert.assertThat(
                "Expected validation error not found: field='" + expectedError.getField()
                    + "', code='" + expectedError.getCode() + "'",
                found,
                Matchers.is(true)
            );
        }
    }

    // ============================================================
    // Internal helpers — JSON
    // ============================================================

    /**
     * Loads a golden file from the classpath and parses it into a {@link JsonNode}.
     *
     * @param fileName file name relative to the {@code golden/} resource directory
     * @return the parsed {@link JsonNode}
     * @throws UncheckedIOException if the resource cannot be found or read
     */
    private static JsonNode loadGoldenNode(final String fileName) {
        final String resourcePath = GOLDEN_PATH + fileName;
        try (InputStream stream = ContractVerifier.class.getClassLoader().getResourceAsStream(resourcePath)) {
            if (stream == null) {
                throw new UncheckedIOException(
                    "Golden file resource not found on classpath: " + resourcePath,
                    new IOException(resourcePath)
                );
            }
            return MAPPER.readTree(stream);
        } catch (final IOException exception) {
            throw new UncheckedIOException("Failed to load golden file: " + resourcePath, exception);
        }
    }

    /**
     * Serialises {@code object} to JSON and parses the result into a {@link JsonNode}.
     *
     * @param object the object to convert
     * @return the corresponding {@link JsonNode}
     */
    private static JsonNode toJsonNode(final Object object) {
        final String json = MAPPER.writeValueAsString(object);
        return MAPPER.readTree(json);
    }

    // ============================================================
    // Internal helpers — reflection
    // ============================================================

    /**
     * Resolves an integer field from the target object by invoking the first matching getter.
     *
     * @param target      the object to introspect
     * @param methodNames candidate getter method names, tried in order
     * @return the integer value returned by the first matching getter
     * @throws AssertionError if none of the candidate methods are found or invocation fails
     */
    private static int resolveIntField(final Object target, final String... methodNames) {
        for (final String methodName : methodNames) {
            final Object result = invokeMethod(target, methodName);
            if (result instanceof final Integer intValue) {
                return intValue;
            }
        }
        throw new AssertionError(buildResolutionError("an integer field", target, methodNames));
    }

    /**
     * Resolves a {@link String} field from the target object by invoking the first matching getter.
     *
     * @param target      the object to introspect
     * @param methodNames candidate getter method names, tried in order
     * @return the string value returned by the first matching getter, or {@code null} if the method returns null
     * @throws AssertionError if none of the candidate methods are found or invocation fails
     */
    private static String resolveStringField(final Object target, final String... methodNames) {
        for (final String methodName : methodNames) {
            try {
                final Object result = target.getClass().getMethod(methodName).invoke(target);
                return result == null ? null : result.toString();
            } catch (final ReflectiveOperationException ignored) {
                // Try the next candidate
            }
        }
        throw new AssertionError(buildResolutionError("a String field", target, methodNames));
    }

    /**
     * Resolves a {@link String} field from the target object silently, returning {@code null}
     * if the method is not found or invocation fails.
     *
     * @param target     the object to introspect
     * @param methodName the getter method name to try
     * @return the string value, or {@code null} if unavailable
     */
    private static String resolveStringFieldSilently(final Object target, final String methodName) {
        final Object result = invokeMethod(target, methodName);
        return result == null ? null : result.toString();
    }

    /**
     * Resolves a {@link List} field from the target object by invoking the first matching getter.
     *
     * @param target      the object to introspect
     * @param methodNames candidate getter method names, tried in order
     * @return the list returned by the first matching getter, never {@code null}
     * @throws AssertionError if none of the candidate methods are found or the result is not a list
     */
    private static List<?> resolveListField(final Object target, final String... methodNames) {
        for (final String methodName : methodNames) {
            final Object result = invokeMethod(target, methodName);
            if (result instanceof final List<?> list) {
                return list;
            }
        }
        throw new AssertionError(buildResolutionError("a List field", target, methodNames));
    }

    /**
     * Invokes a no-arg method on {@code target} by name, returning the result or {@code null}
     * if the method does not exist or invocation fails.
     *
     * @param target     the object to introspect
     * @param methodName the method name to invoke
     * @return the method return value, or {@code null} on any reflective failure
     */
    private static Object invokeMethod(final Object target, final String methodName) {
        try {
            return target.getClass().getMethod(methodName).invoke(target);
        } catch (final ReflectiveOperationException ignored) {
            return null;
        }
    }

    /**
     * Builds the {@link AssertionError} message for failed field resolution.
     *
     * @param typeName    human-readable description of the expected field type
     * @param target      the object that was introspected
     * @param methodNames the candidate method names that were tried
     * @return the formatted error message
     */
    private static String buildResolutionError(
        final String typeName,
        final Object target,
        final String... methodNames) {
        return "Could not resolve " + typeName + " from '" + target.getClass().getSimpleName()
            + USING_METHODS_SUFFIX + String.join(METHOD_NAMES_DELIMITER, methodNames);
    }
}
