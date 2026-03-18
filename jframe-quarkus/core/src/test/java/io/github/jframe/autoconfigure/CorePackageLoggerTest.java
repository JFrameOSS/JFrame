package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.ApplicationConfig;
import io.github.jframe.logging.filter.JFrameFilter;
import io.github.support.UnitTest;

import java.util.ArrayList;
import java.util.List;
import java.util.TimeZone;
import jakarta.enterprise.inject.Instance;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.mockito.Mock;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.hamcrest.Matchers.not;
import static org.hamcrest.Matchers.startsWith;
import static org.mockito.Mockito.when;

/**
 * Tests for {@link CorePackageLogger} — startup log message builder.
 *
 * <p>Tests the package-private {@code buildStartupMessage()} method which assembles
 * the formatted startup log string. This approach allows testing the message content
 * without needing to capture SLF4J log output.
 *
 * <p>All tests follow Given/When/Then with {@link ApplicationConfig} and
 * {@link Instance}&lt;{@link JFrameFilter}&gt; mocked via Mockito.
 */
@DisplayName("Quarkus Core - CorePackageLogger")
public class CorePackageLoggerTest extends UnitTest {

    // -------------------------------------------------------------------------
    // Test fixtures — minimal JFrameFilter implementations
    // -------------------------------------------------------------------------

    /** First mock filter — used for multi-filter ordering tests. */
    static class MockFilter1 implements JFrameFilter {}


    /** Second mock filter — used for multi-filter ordering tests. */
    static class MockFilter2 implements JFrameFilter {}

    // -------------------------------------------------------------------------
    // Subject under test and collaborators
    // -------------------------------------------------------------------------

    @Mock
    private ApplicationConfig applicationConfig;

    @Mock
    private Instance<JFrameFilter> filters;

    private CorePackageLogger corePackageLogger;

    @Override
    @BeforeEach
    public void setUp() {
        super.setUp();
        corePackageLogger = new CorePackageLogger(applicationConfig, filters);
    }

    // -------------------------------------------------------------------------
    // Header line
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should start with application-started header")
    public void shouldStartWithApplicationStartedHeader() {
        // Given: Config with minimal valid values
        when(applicationConfig.name()).thenReturn("any-app");
        when(applicationConfig.group()).thenReturn("any-group");
        when(applicationConfig.version()).thenReturn("0.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message opens with the expected header
        assertThat(message, startsWith("Application started with the following properties:\n"));
    }

    // -------------------------------------------------------------------------
    // Application name
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log application name")
    public void shouldLogApplicationName() {
        // Given: Config with name "test-app"
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message contains the application name
        assertThat(message, containsString("Application Name: test-app"));
    }

    // -------------------------------------------------------------------------
    // Application group
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log application group")
    public void shouldLogApplicationGroup() {
        // Given: Config with group "test-group"
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message contains the application group
        assertThat(message, containsString("Application Group: test-group"));
    }

    // -------------------------------------------------------------------------
    // Application environment
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log application environment")
    public void shouldLogApplicationEnvironment() {
        // Given: Config with environment "prod"
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("prod");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message contains the application environment
        assertThat(message, containsString("Application Environment: prod"));
    }

    // -------------------------------------------------------------------------
    // Application version
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log application version")
    public void shouldLogApplicationVersion() {
        // Given: Config with version "1.0.0"
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message contains the application version
        assertThat(message, containsString("Application Version: 1.0.0"));
    }

    // -------------------------------------------------------------------------
    // Default timezone
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log default timezone using JVM's current default timezone")
    public void shouldLogDefaultTimezone() {
        // Given: Config with any valid values
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Message contains the JVM default timezone ID
        assertThat(message, containsString("Default Timezone: " + TimeZone.getDefault().getID()));
    }

    // -------------------------------------------------------------------------
    // Registered filters — section header always present
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log registered filters section header")
    public void shouldLogRegisteredFiltersSectionHeader() {
        // Given: Config with no filters registered
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Registered Filters section header is always present
        assertThat(message, containsString("Registered Filters:"));
    }

    // -------------------------------------------------------------------------
    // Registered filters — empty list
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log no filter entries when no filters are registered")
    public void shouldLogNoFiltersWhenNoneRegistered() {
        // Given: Empty filter instance
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: No filter entry lines ("      * ") appear
        assertThat(message, not(containsString("      * ")));
    }

    // -------------------------------------------------------------------------
    // Registered filters — multiple filters in iteration order
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should log each registered filter by simple class name")
    public void shouldLogRegisteredFilters() {
        // Given: Two registered JFrameFilter implementations
        when(applicationConfig.name()).thenReturn("test-app");
        when(applicationConfig.group()).thenReturn("test-group");
        when(applicationConfig.version()).thenReturn("1.0.0");
        when(applicationConfig.environment()).thenReturn("dev");
        final List<JFrameFilter> filterList = new ArrayList<>();
        filterList.add(new MockFilter1());
        filterList.add(new MockFilter2());
        when(filters.iterator()).thenReturn(filterList.iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Both filter simple names appear as bullet entries
        assertThat(message, containsString("Registered Filters:"));
        assertThat(message, containsString("      * MockFilter1"));
        assertThat(message, containsString("      * MockFilter2"));
    }

    // -------------------------------------------------------------------------
    // Indentation — each property line uses "  - " prefix
    // -------------------------------------------------------------------------

    @Test
    @DisplayName("Should use correct indentation for each property line")
    public void shouldUseCorrectIndentation() {
        // Given: Config with identifiable values on each property
        when(applicationConfig.name()).thenReturn("my-app");
        when(applicationConfig.group()).thenReturn("my-group");
        when(applicationConfig.version()).thenReturn("2.0.0");
        when(applicationConfig.environment()).thenReturn("staging");
        when(filters.iterator()).thenReturn(new ArrayList<JFrameFilter>().iterator());

        // When: Building the startup message
        final String message = corePackageLogger.buildStartupMessage();

        // Then: Each labelled property starts with the "  - " indent prefix
        assertThat(message, containsString("  - Application Name:"));
        assertThat(message, containsString("  - Application Group:"));
        assertThat(message, containsString("  - Application Environment:"));
        assertThat(message, containsString("  - Application Version:"));
        assertThat(message, containsString("  - Default Timezone:"));
        assertThat(message, containsString("  - Registered Filters:"));
    }
}
