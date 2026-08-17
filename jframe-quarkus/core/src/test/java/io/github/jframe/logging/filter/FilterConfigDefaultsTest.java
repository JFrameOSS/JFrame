package io.github.jframe.logging.filter;

import io.github.support.UnitTest;
import io.smallrye.config.SmallRyeConfigBuilder;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Contract tests for {@link FilterConfig} default values.
 *
 * <p>Verifies the three-way behaviour for each filter toggle:
 * <ul>
 * <li>Property absent → its declared default</li>
 * <li>Property explicitly {@code true} → {@code true}</li>
 * <li>Property explicitly {@code false} → {@code false}</li>
 * </ul>
 *
 * <p>ON by default: request-duration, request-response, user-identity.
 * OFF by default: request-id, transaction-id, outbound-correlation, outbound-logging.
 *
 * <p>These tests use the SmallRye {@link SmallRyeConfigBuilder} to build
 * a real config instance from the {@code @WithDefault} annotations on the
 * interface — no mock, no {@code microprofile-config.properties}.
 */
@DisplayName("Config Defaults - FilterConfig")
public class FilterConfigDefaultsTest extends UnitTest {

    // ======================== HELPERS ========================

    /**
     * Builds a {@link FilterConfig} from SmallRye with no properties set,
     * so only {@code @WithDefault} annotations drive the values.
     */
    private FilterConfig buildDefaultConfig() {
        return new SmallRyeConfigBuilder()
            .addDefaultSources()
            .withMapping(FilterConfig.class)
            .build()
            .getConfigMapping(FilterConfig.class);
    }

    // ======================== FILTERS OFF BY DEFAULT ========================

    @Nested
    @DisplayName("Filters OFF by default (absent = false)")
    class FiltersOffByDefault {

        @Test
        @DisplayName("Should return false for transaction-id filter by default (absent = OFF)")
        public void shouldReturnFalseForTransactionIdByDefault() {
            // Given: No explicit config for transaction-id filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().transactionId().enabled();

            // Then: Filter is OFF by default
            assertThat(enabled, is(false));
        }

        @Test
        @DisplayName("Should return false for request-id filter by default (absent = OFF)")
        public void shouldReturnFalseForRequestIdByDefault() {
            // Given: No explicit config for request-id filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().requestId().enabled();

            // Then: Filter is OFF by default
            assertThat(enabled, is(false));
        }

        @Test
        @DisplayName("Should return false for outbound-correlation filter by default (absent = OFF)")
        public void shouldReturnFalseForOutboundCorrelationByDefault() {
            // Given: No explicit config for outbound-correlation filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().outboundCorrelation().enabled();

            // Then: Filter is OFF by default
            assertThat(enabled, is(false));
        }

        @Test
        @DisplayName("Should return false for outbound-logging filter by default (absent = OFF)")
        public void shouldReturnFalseForOutboundLoggingByDefault() {
            // Given: No explicit config for outbound-logging filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().outboundLogging().enabled();

            // Then: Filter is OFF by default
            assertThat(enabled, is(false));
        }
    }

    // ======================== FILTERS ON BY DEFAULT ========================


    @Nested
    @DisplayName("Filters ON by default (absent = true)")
    class FiltersOnByDefault {

        @Test
        @DisplayName("Should return true for request-duration filter by default (absent = ON)")
        public void shouldReturnTrueForRequestDurationByDefault() {
            // Given: No explicit config for request-duration filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().requestDuration().enabled();

            // Then: Filter is ON by default
            assertThat(enabled, is(true));
        }

        @Test
        @DisplayName("Should return true for request-response filter by default (absent = ON)")
        public void shouldReturnTrueForRequestResponseByDefault() {
            // Given: No explicit config for request-response filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().requestResponse().enabled();

            // Then: Filter is ON by default
            assertThat(enabled, is(true));
        }

        @Test
        @DisplayName("Should return true for user-identity filter by default (absent = ON)")
        public void shouldReturnTrueForUserIdentityByDefault() {
            // Given: No explicit config for user-identity filter

            // When: Reading default value
            final boolean enabled = buildDefaultConfig().userIdentity().enabled();

            // Then: Filter is ON by default
            assertThat(enabled, is(true));
        }
    }

    // ======================== EXPLICIT ENABLE / DISABLE ========================


    @Nested
    @DisplayName("Explicit enablement via system property")
    class ExplicitEnable {

        @Test
        @DisplayName("Should return true for transaction-id filter when explicitly enabled")
        public void shouldReturnTrueForTransactionIdWhenExplicitlyEnabled() {
            // Given: transaction-id filter explicitly enabled
            System.setProperty("jframe.logging.filters.transaction-id.enabled", "true");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().transactionId().enabled();

                // Then: Filter is ON
                assertThat(enabled, is(true));
            } finally {
                System.clearProperty("jframe.logging.filters.transaction-id.enabled");
            }
        }

        @Test
        @DisplayName("Should return true for request-response filter when explicitly enabled")
        public void shouldReturnTrueForRequestResponseWhenExplicitlyEnabled() {
            // Given: request-response filter explicitly enabled
            System.setProperty("jframe.logging.filters.request-response.enabled", "true");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().requestResponse().enabled();

                // Then: Filter is ON
                assertThat(enabled, is(true));
            } finally {
                System.clearProperty("jframe.logging.filters.request-response.enabled");
            }
        }

        @Test
        @DisplayName("Should return false for request-response filter when explicitly disabled")
        public void shouldReturnFalseForRequestResponseWhenExplicitlyDisabled() {
            // Given: request-response filter explicitly disabled
            System.setProperty("jframe.logging.filters.request-response.enabled", "false");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().requestResponse().enabled();

                // Then: Filter is OFF
                assertThat(enabled, is(false));
            } finally {
                System.clearProperty("jframe.logging.filters.request-response.enabled");
            }
        }

        @Test
        @DisplayName("Should return false for request-duration filter when explicitly disabled")
        public void shouldReturnFalseForRequestDurationWhenExplicitlyDisabled() {
            // Given: request-duration filter explicitly disabled
            System.setProperty("jframe.logging.filters.request-duration.enabled", "false");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().requestDuration().enabled();

                // Then: Filter is OFF
                assertThat(enabled, is(false));
            } finally {
                System.clearProperty("jframe.logging.filters.request-duration.enabled");
            }
        }

        @Test
        @DisplayName("Should return false for user-identity filter when explicitly disabled")
        public void shouldReturnFalseForUserIdentityWhenExplicitlyDisabled() {
            // Given: user-identity filter explicitly disabled
            System.setProperty("jframe.logging.filters.user-identity.enabled", "false");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().userIdentity().enabled();

                // Then: Filter is OFF
                assertThat(enabled, is(false));
            } finally {
                System.clearProperty("jframe.logging.filters.user-identity.enabled");
            }
        }

        @Test
        @DisplayName("Should return true for user-identity filter when explicitly enabled")
        public void shouldReturnTrueForUserIdentityWhenExplicitlyEnabled() {
            // Given: user-identity filter explicitly enabled
            System.setProperty("jframe.logging.filters.user-identity.enabled", "true");
            try {
                // When: Reading the property
                final boolean enabled = buildDefaultConfig().userIdentity().enabled();

                // Then: Filter is ON
                assertThat(enabled, is(true));
            } finally {
                System.clearProperty("jframe.logging.filters.user-identity.enabled");
            }
        }
    }
}
