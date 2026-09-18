package io.github.jframe.autoconfigure;

import tools.jackson.databind.ObjectMapper;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Regression guard for the logback-appender wiring defect.
 *
 * <p>The defect: {@code otel.instrumentation.logback-appender.enabled} was hardcoded to
 * {@code false} in {@code jframe-properties.yml} instead of being driven by
 * {@code ${jframe.otlp.logs.enabled}}. This made {@code jframe.otlp.logs.enabled} inert
 * on Spring — the tap was welded shut regardless of the toggle.
 *
 * <p>Tests in this class FAIL against the current (unfixed) YAML and PASS once the
 * placeholder is wired.
 *
 * <p><strong>Two concerns tested:</strong>
 * <ol>
 * <li><em>Source-level guard</em> — the raw YAML text must contain the placeholder string,
 * not a hardcoded literal. This fails today.</li>
 * <li><em>Resolved-value guard</em> — via {@link WebApplicationContextRunner} with
 * {@link CoreAutoConfiguration} active (which loads {@code jframe-properties.yml} through
 * {@code @PropertySource}), the resolved value of the property must follow the toggle.
 * These also fail today because the YAML hardcodes {@code false}.</li>
 * </ol>
 */
@DisplayName("LogbackAppender wiring — otel.instrumentation.logback-appender.enabled follows jframe.otlp.logs.enabled")
class LogbackAppenderWiringTest {

    /** Path from the {@code jframe-spring/core} module root to the YAML defaults file. */
    private static final String YAML_PATH =
        "src/main/resources/jframe-properties.yml";

    /** The YAML key whose resolved value is under test. */
    private static final String APPENDER_KEY =
        "otel.instrumentation.logback-appender.enabled";

    /** The jframe toggle that must drive the appender. */
    private static final String LOGS_TOGGLE =
        "jframe.otlp.logs.enabled";

    /** The exact placeholder string that must appear in the YAML source. */
    private static final String EXPECTED_PLACEHOLDER =
        "${jframe.otlp.logs.enabled}";

    private static final String APP_NAME = "jframe.application.name=test-service";
    private static final String APP_GROUP = "jframe.application.group=io.github.jframe";
    private static final String APP_VERSION = "jframe.application.version=0.0.1";

    /**
     * Context runner with {@link CoreAutoConfiguration} — loads the real
     * {@code jframe-properties.yml} exactly as a consuming application would.
     * The {@link ObjectMapper} bean is required by {@link JacksonConfig}.
     */
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withPropertyValues(APP_NAME, APP_GROUP, APP_VERSION);

    // ── Source-level guard ─────────────────────────────────────────────────

    /**
     * Asserts that the YAML source text at the {@code logback-appender.enabled} line
     * contains the placeholder {@code ${jframe.otlp.logs.enabled}} and NOT a hardcoded
     * literal {@code false}.
     *
     * <p>This is the primary regression guard. It fails today because the YAML reads:
     * <pre>{@code
     *     logback-appender:
     *       enabled: false
     * }</pre>
     * and must instead read:
     * <pre>{@code
     *     logback-appender:
     *       enabled: ${jframe.otlp.logs.enabled}
     * }</pre>
     */
    @Test
    @DisplayName("YAML source must contain placeholder, not literal false — regression guard")
    void shouldContainPlaceholderNotHardcodedFalse() {
        // Given: the jframe-properties.yml file on disk
        final Path yamlPath = Paths.get(YAML_PATH).toAbsolutePath().normalize();
        final List<String> lines;
        try {
            lines = Files.readAllLines(yamlPath);
        } catch (final IOException ex) {
            throw new AssertionError(
                "Cannot read jframe-properties.yml at " + yamlPath + ": " + ex.getMessage(),
                ex
            );
        }

        // When: finding the line that configures logback-appender.enabled
        final String appenderLine = lines.stream()
            .filter(line -> line.contains("enabled:") && isUnderLogbackAppenderBlock(lines, line))
            .findFirst()
            .orElseThrow(
                () -> new AssertionError(
                    "Could not find 'logback-appender: enabled:' line in " + yamlPath
                        + ". YAML structure may have changed."
                )
            );

        // Then: the line must contain the placeholder, not the literal 'false'
        assertThat(
            "otel.instrumentation.logback-appender.enabled must use placeholder "
                + EXPECTED_PLACEHOLDER + " not a hardcoded literal. "
                + "Actual YAML line: [" + appenderLine.strip() + "]",
            appenderLine.contains(EXPECTED_PLACEHOLDER),
            is(true)
        );
    }

    /**
     * Returns {@code true} when the given {@code enabledLine} is the line immediately
     * following a {@code logback-appender:} line in the list — i.e. it belongs to the
     * logback-appender block.
     */
    private static boolean isUnderLogbackAppenderBlock(final List<String> lines, final String enabledLine) {
        final int idx = lines.indexOf(enabledLine);
        for (int i = idx - 1; i >= 0; i--) {
            final String prev = lines.get(i).strip();
            if (prev.isEmpty() || prev.startsWith("#")) {
                continue;
            }
            return prev.startsWith("logback-appender:");
        }
        return false;
    }

    // ── Resolved-value guards ──────────────────────────────────────────────

    @Nested
    @DisplayName("Resolved value follows the toggle")
    class ResolvedValue {

        @Test
        @DisplayName("Should resolve to true when jframe.otlp.logs.enabled=true (default)")
        void shouldResolveTrueWhenToggleIsTrue() {
            // Given: logs.enabled explicitly set to true (matches shipped default)
            contextRunner
                .withPropertyValues(LOGS_TOGGLE + "=true")
                .run(ctx -> {
                    // When: reading the resolved property from the Spring Environment
                    final String resolved = ctx.getEnvironment().getProperty(APPENDER_KEY);

                    // Then: appender must be enabled — log records can flow
                    assertThat(
                        APPENDER_KEY + " must resolve to 'true' when " + LOGS_TOGGLE + "=true. "
                            + "Actual: [" + resolved + "]. "
                            + "The YAML hardcodes 'false' instead of using the placeholder.",
                        resolved,
                        is("true")
                    );
                });
        }

        @Test
        @DisplayName("Should resolve to false when jframe.otlp.logs.enabled=false")
        void shouldResolveFalseWhenToggleIsFalse() {
            // Given: consumer explicitly disables log export
            contextRunner
                .withPropertyValues(LOGS_TOGGLE + "=false")
                .run(ctx -> {
                    // When: reading the resolved property from the Spring Environment
                    final String resolved = ctx.getEnvironment().getProperty(APPENDER_KEY);

                    // Then: appender must be disabled — no records produced
                    assertThat(
                        APPENDER_KEY + " must resolve to 'false' when " + LOGS_TOGGLE + "=false. "
                            + "Actual: [" + resolved + "].",
                        resolved,
                        is("false")
                    );
                });
        }

        @Test
        @DisplayName("Should resolve to true when jframe.otlp.logs.enabled is absent (shipped default)")
        void shouldResolveTrueWhenToggleIsAbsent() {
            // Given: no per-signal override — jframe-properties.yml default of true applies
            contextRunner.run(ctx -> {
                // When: reading the resolved property from the Environment
                final String resolved = ctx.getEnvironment().getProperty(APPENDER_KEY);

                // Then: shipped default is logs.enabled=true, so appender must be enabled
                assertThat(
                    APPENDER_KEY + " must resolve to 'true' by default (jframe.otlp.logs.enabled defaults to true). "
                        + "Actual: [" + resolved + "]. "
                        + "The YAML hardcodes 'false', overriding the shipped default.",
                    resolved,
                    is("true")
                );
            });
        }
    }
}
