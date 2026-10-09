package io.github.support;

import com.networknt.schema.Error;
import com.networknt.schema.InputFormat;
import com.networknt.schema.Schema;
import com.networknt.schema.SchemaRegistry;
import com.networknt.schema.SchemaRegistryConfig;
import com.networknt.schema.SpecificationVersion;
import tools.jackson.core.type.TypeReference;
import tools.jackson.databind.json.JsonMapper;

import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.net.URI;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.anyOf;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.equalTo;
import static org.hamcrest.Matchers.instanceOf;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.nullValue;

/**
 * Shared RFC 9457 test support: wire-format parsing, Appendix A schema check and cross-runtime expected bodies.
 */
public final class ProblemJson {

    /** Problem Details media type. */
    public static final String PROBLEM_JSON = "application/problem+json";

    /** Members whose values differ per request; ignored when comparing to expected bodies. */
    public static final Set<String> CORRELATION_MEMBERS = Set.of("txId", "traceId", "spanId");

    private static final String STATUS = "status";

    private static final JsonMapper MAPPER = JsonMapper.builder().build();

    private static final Schema SCHEMA = loadSchema();

    private ProblemJson() {
    }

    /** Serialises the given object and reads it back as a map. */
    public static Map<String, Object> toMap(final Object body) {
        return parse(MAPPER.writeValueAsString(body));
    }

    /** Serialises the given object to JSON. */
    public static String toJson(final Object body) {
        return MAPPER.writeValueAsString(body);
    }

    /** Parses a JSON string into a map. */
    public static Map<String, Object> parse(final String json) {
        return MAPPER.readValue(json, new TypeReference<>() {
        });
    }

    /**
     * Asserts the body satisfies RFC 9457: Appendix A schema plus member types, {@code status} equal to the HTTP status.
     *
     * @param json       the serialised body
     * @param httpStatus the HTTP response status, or {@code null} to skip the status equality check
     */
    public static void assertRfc9457(final String json, final Integer httpStatus) {
        final List<Error> errors = SCHEMA.validate(json, InputFormat.JSON);
        assertThat("RFC 9457 Appendix A schema violations: " + errors, errors, is(empty()));

        final Map<String, Object> body = parse(json);
        assertUriReference(body, "type");
        assertUriReference(body, "instance");
        assertOptionalType(body, "title", String.class);
        assertOptionalType(body, "detail", String.class);
        assertOptionalType(body, STATUS, Integer.class);
        if (httpStatus != null) {
            assertThat("status member", body.get(STATUS), anyOf(nullValue(), equalTo(httpStatus)));
        }
    }

    /** Loads a shared expected body ({@code problems/expected/<name>.json}); {@code type} omitted (no base URI configured). */
    public static Map<String, Object> expected(final String name) {
        return parse(read("problems/expected/" + name + ".json"));
    }

    /** Loads a shared expected body with {@code type} = base URI + percent-encoded {@code errorCode}. */
    public static Map<String, Object> expected(final String name, final String typeBaseUri) {
        final Map<String, Object> body = new LinkedHashMap<>(expected(name));
        body.put("type", typeBaseUri + URLEncoder.encode((String) body.get("errorCode"), StandardCharsets.UTF_8).replace("+", "%20"));
        return body;
    }

    /** Asserts the body equals the shared expected body (no {@code type}), ignoring {@link #CORRELATION_MEMBERS}. */
    public static void assertMatchesExpected(final String json, final String name) {
        assertEqualsIgnoringCorrelation(json, expected(name), name);
    }

    /** Asserts the body equals the shared expected body with the configured {@code type} base URI. */
    public static void assertMatchesExpected(final String json, final String name, final String typeBaseUri) {
        assertEqualsIgnoringCorrelation(json, expected(name, typeBaseUri), name);
    }

    private static void assertEqualsIgnoringCorrelation(final String json, final Map<String, Object> expectedBody, final String name) {
        final Map<String, Object> expected = withoutCorrelation(expectedBody);
        assertThat("body equals shared expected " + name, withoutCorrelation(parse(json)), is(equalTo(expected)));
    }

    private static Map<String, Object> withoutCorrelation(final Map<String, Object> body) {
        final Map<String, Object> copy = new LinkedHashMap<>(body);
        CORRELATION_MEMBERS.forEach(copy::remove);
        return copy;
    }

    private static void assertUriReference(final Map<String, Object> body, final String member) {
        final Object value = body.get(member);
        if (value != null) {
            assertThat(member + " must be a string", value, instanceOf(String.class));
            URI.create((String) value);
        }
    }

    private static void assertOptionalType(final Map<String, Object> body, final String member, final Class<?> type) {
        final Object value = body.get(member);
        if (value != null) {
            assertThat(member + " type", value, instanceOf(type));
        }
    }

    private static Schema loadSchema() {
        final SchemaRegistry registry = SchemaRegistry.withDefaultDialect(
            SpecificationVersion.DRAFT_2020_12,
            builder -> builder.schemaRegistryConfig(SchemaRegistryConfig.builder().formatAssertionsEnabled(true).build())
        );
        return registry.getSchema(read("problems/rfc9457-problem.schema.json"), InputFormat.JSON);
    }

    private static String read(final String resource) {
        try (InputStream stream = ProblemJson.class.getClassLoader().getResourceAsStream(resource)) {
            if (stream == null) {
                throw new IllegalStateException("Missing test resource: " + resource);
            }
            return new String(stream.readAllBytes(), StandardCharsets.UTF_8);
        } catch (final IOException exception) {
            throw new UncheckedIOException(exception);
        }
    }
}
