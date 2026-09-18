package io.github.jframe.tracing;

import io.github.support.UnitTest;

import java.io.IOException;
import java.io.InputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.Map;
import java.util.Properties;
import java.util.Set;
import java.util.TreeSet;
import java.util.stream.Collectors;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.empty;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.not;

/**
 * Cross-runtime configuration parity guard for {@code jframe.otlp.*} defaults.
 *
 * <p>Enforces that the Spring Boot and Quarkus runtime resource files express the same
 * {@code jframe.otlp.*} default values as each other AND as the constants in
 * {@link OtlpDefaults} (the single source of truth).
 *
 * <p>This test exists because the two files previously diverged silently — telemetry was
 * disabled by default in one runtime and enabled in the other, and a sampler that ignored
 * parent sampling decisions was used. A comment in the Quarkus file claimed parity but
 * nothing enforced it.
 *
 * <p><strong>Design note:</strong> This test lives in {@code jframe-core} because
 * {@link OtlpDefaults} lives here and is the single source of truth. The test reads the
 * two resource files from disk via relative paths from the module root — no framework
 * dependencies needed.
 *
 * <p><strong>Future-proof key detection:</strong> The test collects ALL {@code jframe.otlp.*}
 * keys present in either file and asserts that every key appears in both. A key added to one
 * file but not the other will cause an explicit failure naming the missing key.
 */
@DisplayName("OtlpDefaults - cross-runtime configuration parity")
class OtlpConfigParityTest extends UnitTest {

    /**
     * Path from the {@code jframe-core} module root to the Spring YAML defaults file.
     * Gradle always runs tests from the module root directory.
     */
    private static final String SPRING_YAML_PATH =
        "../jframe-spring/core/src/main/resources/jframe-properties.yml";

    /**
     * Path from the {@code jframe-core} module root to the Quarkus properties defaults file.
     */
    private static final String QUARKUS_PROPERTIES_PATH =
        "../jframe-quarkus/otlp/src/main/resources/META-INF/microprofile-config.properties";

    /** Key prefix scope: only keys in this namespace are compared. */
    private static final String JFRAME_OTLP_PREFIX = "jframe.otlp.";

    // ======================== PARITY ASSERTIONS ========================

    @Test
    @DisplayName("Should agree on disabled default — both runtimes must require explicit opt-in")
    public void shouldAgreeOnDisabledDefault() {
        // Given: Both runtime config files parsed into flat key-value maps
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.disabled";

        // When: Reading the disabled key from each file
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(
            formatMismatch(key, springValue, quarkusValue),
            quarkusValue,
            is(springValue)
        );
        assertThat(
            formatConstantMismatch(key, springValue, String.valueOf(OtlpDefaults.DEFAULT_DISABLED)),
            Boolean.parseBoolean(springValue),
            is(OtlpDefaults.DEFAULT_DISABLED)
        );
    }

    @Test
    @DisplayName("Should agree on url default — both runtimes point to the same OTLP endpoint")
    public void shouldAgreeOnUrlDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.url";

        // When: Reading the url key
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, OtlpDefaults.DEFAULT_URL),
            springValue,
            is(OtlpDefaults.DEFAULT_URL)
        );
    }

    @Test
    @DisplayName("Should agree on timeout default — both runtimes use the same connection timeout")
    public void shouldAgreeOnTimeoutDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.timeout";

        // When: Reading the timeout key
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, OtlpDefaults.DEFAULT_TIMEOUT),
            springValue,
            is(OtlpDefaults.DEFAULT_TIMEOUT)
        );
    }

    @Test
    @DisplayName("Should agree on exporter default — both runtimes use the same exporter protocol")
    public void shouldAgreeOnExporterDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.exporter";

        // When: Reading the exporter key
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, OtlpDefaults.DEFAULT_EXPORTER),
            springValue,
            is(OtlpDefaults.DEFAULT_EXPORTER)
        );
    }

    @Test
    @DisplayName("Should agree on sampling-rate default — both runtimes sample at the same rate")
    public void shouldAgreeOnSamplingRateDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.sampling-rate";

        // When: Reading the sampling-rate key and normalising to double
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree by numeric value (not raw string), and agree with constant
        final double springRate = Double.parseDouble(springValue);
        final double quarkusRate = Double.parseDouble(quarkusValue);
        assertThat(
            formatMismatch(key, springValue, quarkusValue),
            quarkusRate,
            is(springRate)
        );
        assertThat(
            formatConstantMismatch(key, springValue, String.valueOf(OtlpDefaults.DEFAULT_SAMPLING_RATE)),
            springRate,
            is(OtlpDefaults.DEFAULT_SAMPLING_RATE)
        );
    }

    @Test
    @DisplayName("Should agree on sampler-type default — both runtimes respect parent sampling decision")
    public void shouldAgreeOnSamplerTypeDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.sampler-type";

        // When: Reading the sampler-type key
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, OtlpDefaults.DEFAULT_SAMPLER_TYPE),
            springValue,
            is(OtlpDefaults.DEFAULT_SAMPLER_TYPE)
        );
    }

    @Test
    @DisplayName("Should agree on excluded-methods default — both runtimes exclude the same set of paths")
    public void shouldAgreeOnExcludedMethodsDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.excluded-methods";

        // When: Reading excluded-methods and normalising to a sorted set (Spring YAML list → CSV)
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        final Set<String> springSet = normaliseToSet(springValue);
        final Set<String> quarkusSet = normaliseToSet(quarkusValue);
        final Set<String> constantSet = OtlpDefaults.parseCommaSeparated(
            OtlpDefaults.DEFAULT_EXCLUDED_METHODS
        );

        // Then: All three agree on the same set of excluded paths
        assertThat(
            "Key [" + key + "]: Quarkus set " + quarkusSet + " differs from Spring set " + springSet,
            quarkusSet,
            is(springSet)
        );
        assertThat(
            "Key [" + key + "]: Spring set " + springSet + " differs from OtlpDefaults constant " + constantSet,
            springSet,
            is(constantSet)
        );
    }

    @Test
    @DisplayName("Should agree on excluded-resource-attributes default — both runtimes hide the same process keys")
    public void shouldAgreeOnExcludedResourceAttributesDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.excluded-resource-attributes";

        // When: Reading and normalising to a set of strings
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        final Set<String> springSet = normaliseToSet(springValue);
        final Set<String> quarkusSet = normaliseToSet(quarkusValue);
        final Set<String> constantSet = OtlpDefaults.parseCommaSeparated(
            OtlpDefaults.DEFAULT_EXCLUDED_RESOURCE_ATTRIBUTES
        );

        // Then: All three agree
        assertThat(
            "Key [" + key + "]: Quarkus set " + quarkusSet + " differs from Spring set " + springSet,
            quarkusSet,
            is(springSet)
        );
        assertThat(
            "Key [" + key + "]: Spring set " + springSet + " differs from OtlpDefaults constant " + constantSet,
            springSet,
            is(constantSet)
        );
    }

    @Test
    @DisplayName("Should agree on traces.enabled default — both runtimes enable the traces signal")
    public void shouldAgreeOnTracesEnabledDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.traces.enabled";

        // When: Reading traces.enabled
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, String.valueOf(OtlpDefaults.DEFAULT_TRACES_ENABLED)),
            Boolean.parseBoolean(springValue),
            is(OtlpDefaults.DEFAULT_TRACES_ENABLED)
        );
    }

    @Test
    @DisplayName("Should agree on metrics.enabled default — both runtimes enable the metrics signal")
    public void shouldAgreeOnMetricsEnabledDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.metrics.enabled";

        // When: Reading metrics.enabled
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, String.valueOf(OtlpDefaults.DEFAULT_METRICS_ENABLED)),
            Boolean.parseBoolean(springValue),
            is(OtlpDefaults.DEFAULT_METRICS_ENABLED)
        );
    }

    @Test
    @DisplayName("Should agree on logs.enabled default — both runtimes enable the logs signal")
    public void shouldAgreeOnLogsEnabledDefault() {
        // Given: Both runtime config files parsed
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();
        final String key = "jframe.otlp.logs.enabled";

        // When: Reading logs.enabled
        final String springValue = requireKey(spring, key, "Spring YAML");
        final String quarkusValue = requireKey(quarkus, key, "Quarkus properties");

        // Then: Both agree, and both agree with OtlpDefaults constant
        assertThat(formatMismatch(key, springValue, quarkusValue), quarkusValue, is(springValue));
        assertThat(
            formatConstantMismatch(key, springValue, String.valueOf(OtlpDefaults.DEFAULT_LOGS_ENABLED)),
            Boolean.parseBoolean(springValue),
            is(OtlpDefaults.DEFAULT_LOGS_ENABLED)
        );
    }

    @Test
    @DisplayName("Should have a live log-record path on both runtimes when logs.enabled=true — closes the tap/drain blind spot")
    public void shouldHaveLiveLogPathOnBothRuntimesWhenLogsEnabled() {
        // Given: Both runtime config files parsed to flat maps
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();

        // Precondition: both agree that logs are enabled (existing test covers this,
        // but we assert it here explicitly so the failure message is clear)
        final String logsEnabledKey = "jframe.otlp.logs.enabled";
        final String springLogsEnabled = requireKey(spring, logsEnabledKey, "Spring YAML");
        assertThat(
            "Precondition: " + logsEnabledKey + " must be true in Spring YAML for this test to be meaningful",
            Boolean.parseBoolean(springLogsEnabled),
            is(true)
        );

        // When: reading the runtime-specific keys that PRODUCE log records (not just export them)

        // Quarkus: quarkus.otel.logs.exporter=cdi means the CDI exporter is active and
        // Quarkus' built-in log handler feeds into it. A non-cdi value (e.g. "none") means
        // no records are produced. We read it directly from the raw Quarkus properties file.
        final Map<String, String> quarkusRaw = parseAllQuarkusProperties();
        final String quarkusLogsExporter = requireKey(
            quarkusRaw,
            "quarkus.otel.logs.exporter",
            "Quarkus properties (quarkus.otel.logs.exporter)"
        );

        // Spring: the logback-appender instrumentation is the source of log records.
        // Its enabled value must be the placeholder ${jframe.otlp.logs.enabled} — NOT a
        // literal 'false'. We verify this by reading the raw YAML line from disk.
        final String logbackAppenderLine = readLogbackAppenderEnabledLine();

        // Then: Quarkus must have a live exporter path (cdi, not none)
        assertThat(
            "quarkus.otel.logs.exporter must be 'cdi' so log records flow when logs.enabled=true. "
                + "Actual: [" + quarkusLogsExporter + "].",
            quarkusLogsExporter,
            is("cdi")
        );

        // Then: Spring must NOT hardcode 'false' for the logback appender.
        // The value must be the placeholder so it follows jframe.otlp.logs.enabled.
        final String expectedPlaceholder = "${jframe.otlp.logs.enabled}";
        assertThat(
            "otel.instrumentation.logback-appender.enabled in jframe-properties.yml must be "
                + "the placeholder [" + expectedPlaceholder + "] so it follows jframe.otlp.logs.enabled. "
                + "Actual YAML line content: [" + logbackAppenderLine.strip() + "]. "
                + "A hardcoded 'false' means log records are never produced regardless of the toggle.",
            logbackAppenderLine.contains(expectedPlaceholder),
            is(true)
        );
    }

    @Test
    @DisplayName("Should have identical jframe.otlp.* key sets — future additions to one file must appear in both")
    public void shouldHaveIdenticalKeySetInBothFiles() {
        // Given: Both runtime config files parsed to flat maps
        final Map<String, String> spring = parseSpringYaml();
        final Map<String, String> quarkus = parseQuarkusProperties();

        // When: Computing the symmetric difference (keys in one but not the other)
        final Set<String> springOnly = new TreeSet<>(spring.keySet());
        springOnly.removeAll(quarkus.keySet());

        final Set<String> quarkusOnly = new TreeSet<>(quarkus.keySet());
        quarkusOnly.removeAll(spring.keySet());

        // Then: No divergence — symmetric difference must be empty
        assertThat(
            "Keys present in Spring YAML but MISSING from Quarkus properties: " + springOnly
                + ". Add them to microprofile-config.properties.",
            springOnly,
            is(empty())
        );
        assertThat(
            "Keys present in Quarkus properties but MISSING from Spring YAML: " + quarkusOnly
                + ". Add them to jframe-properties.yml.",
            quarkusOnly,
            is(empty())
        );
    }

    // ======================== PARSING INFRASTRUCTURE ========================

    /**
     * Reads ALL key-value pairs from the Quarkus {@code microprofile-config.properties} file,
     * without filtering by prefix. Used to inspect runtime-specific keys such as
     * {@code quarkus.otel.logs.exporter} that are outside the {@code jframe.otlp.*} scope.
     */
    private Map<String, String> parseAllQuarkusProperties() {
        final Path path = resolveModuleRelativePath(QUARKUS_PROPERTIES_PATH);
        try (InputStream in = Files.newInputStream(path)) {
            final Properties props = new Properties();
            props.load(in);
            final Map<String, String> result = new HashMap<>();
            for (final String name : props.stringPropertyNames()) {
                result.put(name, props.getProperty(name).trim());
            }
            assertThat(
                "Quarkus properties at " + path.toAbsolutePath() + " is empty — path may be wrong",
                result.entrySet(),
                is(not(empty()))
            );
            return result;
        } catch (final IOException ex) {
            throw new AssertionError(
                "Cannot read Quarkus properties at " + path.toAbsolutePath() + ": " + ex.getMessage(),
                ex
            );
        }
    }

    /**
     * Returns the raw YAML line that sets {@code enabled} inside the {@code logback-appender:}
     * block of {@code jframe-properties.yml}. Used to verify the placeholder is present in source.
     */
    private String readLogbackAppenderEnabledLine() {
        final Path path = resolveModuleRelativePath(SPRING_YAML_PATH);
        final java.util.List<String> lines;
        try {
            lines = Files.readAllLines(path);
        } catch (final IOException ex) {
            throw new AssertionError(
                "Cannot read Spring YAML at " + path.toAbsolutePath() + ": " + ex.getMessage(),
                ex
            );
        }

        for (int i = 0; i < lines.size(); i++) {
            if (lines.get(i).strip().startsWith("logback-appender:")) {
                // Look for the 'enabled:' line immediately following (skipping blanks/comments)
                for (int j = i + 1; j < lines.size(); j++) {
                    final String candidate = lines.get(j);
                    final String stripped = candidate.strip();
                    if (stripped.isEmpty() || stripped.startsWith("#")) {
                        continue;
                    }
                    if (stripped.startsWith("enabled:")) {
                        return candidate;
                    }
                    // Any non-blank, non-comment line that isn't 'enabled:' means the block ended
                    break;
                }
            }
        }
        throw new AssertionError(
            "Could not find 'logback-appender: enabled:' entry in " + path.toAbsolutePath()
                + ". YAML structure may have changed."
        );
    }

    /**
     * Parses the Spring Boot {@code jframe-properties.yml} file and returns a flat map
     * containing only {@code jframe.otlp.*} keys.
     *
     * <p>The YAML block for {@code jframe.otlp} has a predictable structure; this
     * hand-written parser handles:
     * <ul>
     * <li>Nested blocks (e.g. {@code traces:\n  enabled: true} → {@code jframe.otlp.traces.enabled})</li>
     * <li>Scalar values ({@code key: value})</li>
     * <li>Block sequence lists ({@code - item}) collected into comma-separated form</li>
     * <li>Quoted strings (surrounding quotes are stripped)</li>
     * </ul>
     *
     * <p>Lines outside the {@code jframe.otlp} section are ignored.
     */
    private Map<String, String> parseSpringYaml() {
        final Path path = resolveModuleRelativePath(SPRING_YAML_PATH);
        try {
            final java.util.List<String> lines = Files.readAllLines(path);
            return parseYamlOtlpSection(lines);
        } catch (final IOException ex) {
            throw new AssertionError(
                "Cannot read Spring YAML at " + path.toAbsolutePath() + ": " + ex.getMessage(),
                ex
            );
        }
    }

    /**
     * Parses the Quarkus {@code microprofile-config.properties} file and returns a flat map
     * containing only {@code jframe.otlp.*} keys.
     *
     * <p>{@code quarkus.otel.*} and all other prefixes are excluded from scope.
     */
    private Map<String, String> parseQuarkusProperties() {
        final Path path = resolveModuleRelativePath(QUARKUS_PROPERTIES_PATH);
        try (InputStream in = Files.newInputStream(path)) {
            final Properties props = new Properties();
            props.load(in);
            final Map<String, String> result = new HashMap<>();
            for (final String name : props.stringPropertyNames()) {
                if (name.startsWith(JFRAME_OTLP_PREFIX)) {
                    result.put(name, props.getProperty(name).trim());
                }
            }
            assertThat(
                "Quarkus properties at " + path.toAbsolutePath()
                    + " contains no jframe.otlp.* keys — file may be empty or path is wrong",
                result.entrySet(),
                is(not(empty()))
            );
            return result;
        } catch (final IOException ex) {
            throw new AssertionError(
                "Cannot read Quarkus properties at " + path.toAbsolutePath() + ": " + ex.getMessage(),
                ex
            );
        }
    }

    /**
     * Resolves a path expressed relative to the {@code jframe-core} module root.
     * Gradle runs tests with the working directory set to the module root, so
     * {@code Paths.get(relative)} works directly. We resolve it to an absolute path
     * for clearer error messages.
     */
    private Path resolveModuleRelativePath(final String relative) {
        return Paths.get(relative).toAbsolutePath().normalize();
    }

    /**
     * Hand-written line-by-line YAML parser for the {@code jframe.otlp.*} section.
     *
     * <p>The parser tracks indentation levels to reconstruct dotted keys. The algorithm:
     * <ol>
     * <li>Ignore lines until we enter the {@code jframe:} → {@code otlp:} block.</li>
     * <li>Once inside, track a stack of (indentLevel, keySegment) to build full keys.</li>
     * <li>A line with a scalar value ({@code key: value}) creates a flat map entry.</li>
     * <li>A list item ({@code - value}) is accumulated into a comma-separated string.</li>
     * <li>When indentation decreases, pop the stack until we match the current level.</li>
     * <li>Exit the {@code jframe.otlp} block when a top-level key is encountered.</li>
     * </ol>
     */
    @SuppressWarnings("PMD.CyclomaticComplexity")
    private Map<String, String> parseYamlOtlpSection(final java.util.List<String> lines) {
        final Map<String, String> result = new HashMap<>();

        // State machine: track whether we are inside the jframe.otlp block
        boolean inJframe = false;
        boolean inOtlp = false;

        // Key stack: each entry is (indentLevel, keySegment)
        // Index 0 = outermost. Stack is rebuilt on every scalar or mapping key line.
        final java.util.Deque<int[]> indentStack = new java.util.ArrayDeque<>();
        // Parallel deque for key segments
        final java.util.Deque<String> keyStack = new java.util.ArrayDeque<>();

        // For accumulating YAML block sequences (- item lines) under one key
        String listKey = null;
        final java.util.List<String> listItems = new java.util.ArrayList<>();

        for (final String rawLine : lines) {
            // Skip blank lines and YAML comments
            if (rawLine.isBlank() || rawLine.stripLeading().startsWith("#")) {
                continue;
            }

            final int indent = countLeadingSpaces(rawLine);
            final String trimmed = rawLine.strip();

            // --- PHASE 1: locate the jframe: block (indent 0) ---
            if (!inJframe) {
                if (trimmed.equals("jframe:") && indent == 0) {
                    inJframe = true;
                }
                continue;
            }

            // --- PHASE 2: locate the otlp: sub-block (indent 2) ---
            if (!inOtlp) {
                if (indent == 0) {
                    // Exited jframe: block to a new top-level key
                    inJframe = false;
                    continue;
                }
                if (trimmed.equals("otlp:") && indent == 2) {
                    inOtlp = true;
                    // Reset key tracking to inside jframe.otlp
                    indentStack.clear();
                    keyStack.clear();
                    listKey = null;
                    listItems.clear();
                }
                continue;
            }

            // --- PHASE 3: inside jframe.otlp ---

            // If we return to indent ≤ 2, we have left the otlp block
            if (indent <= 2 && !trimmed.startsWith("-")) {
                // Flush any pending list
                if (listKey != null && !listItems.isEmpty()) {
                    result.put(listKey, String.join(",", listItems));
                    listKey = null;
                    listItems.clear();
                }
                if (indent == 0) {
                    // Left jframe: entirely
                    break;
                }
                if (indent == 2) {
                    // Could be another key at same level as otlp:, or just the next sibling
                    inOtlp = false;
                    inJframe = false;
                    break;
                }
            }

            // Handle YAML list items: "  - value"
            if (trimmed.startsWith("- ")) {
                final String itemValue = trimmed.substring(2).trim().replace("\"", "").replace("'", "");
                listItems.add(itemValue);
                continue;
            }

            // Flush pending list when we encounter a non-list line
            if (listKey != null && !trimmed.startsWith("- ")) {
                if (!listItems.isEmpty()) {
                    result.put(listKey, String.join(",", listItems));
                }
                listKey = null;
                listItems.clear();
            }

            // Parse a YAML mapping key (possibly with a scalar value)
            final int colonIdx = trimmed.indexOf(':');
            if (colonIdx < 0) {
                continue;
            }

            final String rawKey = trimmed.substring(0, colonIdx).trim();
            final String rawValue = trimmed.substring(colonIdx + 1).trim();

            // Adjust the indent stack — pop entries whose indentation is >= current
            while (!indentStack.isEmpty() && indentStack.peek()[0] >= indent) {
                indentStack.pop();
                keyStack.pop();
            }

            // Push current key segment
            indentStack.push(
                new int[] {
                    indent
                }
            );
            keyStack.push(rawKey);

            // Build full dotted key: jframe.otlp + reversed stack
            final java.util.List<String> segments = new java.util.ArrayList<>(keyStack);
            java.util.Collections.reverse(segments);
            // segments[0] is the deepest ancestor still on stack after the jframe.otlp prefix
            // We remove the leading "otlp" segment since jframe.otlp is our prefix
            // Actually the stack starts INSIDE otlp — the first push is a child of otlp
            // So full key = jframe.otlp + "." + segments.join(".")
            final String fullKey = JFRAME_OTLP_PREFIX + String.join(".", segments);

            if (rawValue.isEmpty()) {
                // This is a mapping key (no value) — children will follow
                // If it could be a list parent, clear listItems
                listKey = fullKey;
                listItems.clear();
            } else {
                // Scalar value — strip surrounding quotes
                final String value = rawValue.replace("\"", "").replace("'", "");
                result.put(fullKey, value);
                // This key has a scalar, not a list — clear any list state
                listKey = null;
                listItems.clear();
            }
        }

        // Flush any trailing list
        if (listKey != null && !listItems.isEmpty()) {
            result.put(listKey, String.join(",", listItems));
        }

        assertThat(
            "Spring YAML at " + resolveModuleRelativePath(SPRING_YAML_PATH).toAbsolutePath()
                + " produced no jframe.otlp.* keys — YAML structure may have changed",
            result.entrySet(),
            is(not(empty()))
        );
        return result;
    }

    // ======================== HELPERS ========================

    private static int countLeadingSpaces(final String line) {
        int count = 0;
        for (int i = 0; i < line.length(); i++) {
            if (line.charAt(i) == ' ') {
                count++;
            } else {
                break;
            }
        }
        return count;
    }

    /**
     * Retrieves a required key from the map, failing with an informative message if absent.
     * "Informative" means naming the exact key and the file where it was expected.
     */
    private static String requireKey(final Map<String, String> map, final String key, final String fileDescription) {
        final String value = map.get(key);
        assertThat(
            "Key [" + key + "] is MISSING from " + fileDescription + ". "
                + "Keys present: " + new TreeSet<>(map.keySet()),
            value != null,
            is(true)
        );
        return value;
    }

    /**
     * Normalises a value to a {@link Set} of trimmed, non-blank strings.
     * Accepts both comma-separated strings (Quarkus) and comma-joined Spring YAML list values
     * (already joined by the parser). Comparison is order-independent.
     */
    private static Set<String> normaliseToSet(final String value) {
        if (value == null || value.isBlank()) {
            return new LinkedHashSet<>();
        }
        return Arrays.stream(value.split(","))
            .map(String::trim)
            .filter(s -> !s.isBlank())
            .collect(Collectors.toCollection(LinkedHashSet::new));
    }

    private static String formatMismatch(final String key, final String springValue, final String quarkusValue) {
        return "Key [" + key + "] DIVERGES between runtimes: "
            + "Spring=" + springValue + " vs Quarkus=" + quarkusValue
            + ". Update microprofile-config.properties or jframe-properties.yml to match.";
    }

    private static String formatConstantMismatch(final String key, final String fileValue, final String constantValue) {
        return "Key [" + key + "] in resource files (" + fileValue
            + ") does not match OtlpDefaults constant (" + constantValue
            + "). Update the constant or both resource files together.";
    }
}
