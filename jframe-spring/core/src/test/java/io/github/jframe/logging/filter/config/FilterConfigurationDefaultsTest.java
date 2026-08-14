package io.github.jframe.logging.filter.config;


import io.github.jframe.logging.logger.RequestResponseLogger;
import io.github.jframe.logging.voter.FilterVoter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.mockito.Mockito.mock;

/**
 * Context-runner tests for Spring filter auto-configuration defaults.
 *
 * <p>Asserts that:
 * <ul>
 * <li><b>request-duration</b> and <b>request-response</b> beans are <b>present</b> when the property is absent (ON by default)</li>
 * <li><b>request-id</b> and <b>transaction-id</b> beans are <b>absent</b> when the property is absent (OFF by default)</li>
 * <li>Every filter is present when the property is explicitly {@code true}</li>
 * <li>Every filter is absent when the property is explicitly {@code false}</li>
 * </ul>
 */
@DisplayName("Filter Configuration - Spring auto-configuration defaults")
public class FilterConfigurationDefaultsTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(
            RequestIdFilterConfiguration.class,
            TransactionIdFilterConfiguration.class,
            RequestDurationFilterConfiguration.class,
            RequestResponseLogFilterConfiguration.class
        )
        .withBean(FilterVoter.class, () -> mock(FilterVoter.class))
        .withBean(RequestResponseLogger.class, () -> mock(RequestResponseLogger.class));

    // ======================== REQUEST-ID FILTER ========================

    @Nested
    @DisplayName("request-id filter")
    class RequestIdFilter {

        @Test
        @DisplayName("Should NOT register RequestIdFilter when property is absent (disabled by default)")
        public void shouldNotRegisterRequestIdFilterWhenPropertyIsAbsent() {
            // Given: No property set (absent = OFF)

            // When / Then: ApplicationContext does not contain RequestIdFilter bean
            contextRunner.run(
                context -> assertThat(context.containsBean("requestIdFilter"), is(false))
            );
        }

        @Test
        @DisplayName("Should register RequestIdFilter when explicitly enabled")
        public void shouldRegisterRequestIdFilterWhenExplicitlyEnabled() {
            // Given: Property explicitly set to true

            // When / Then: ApplicationContext contains RequestIdFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-id.enabled=true")
                .run(
                    context -> assertThat(
                        context.getBeansOfType(io.github.jframe.logging.filter.type.RequestIdFilter.class).isEmpty(),
                        is(false)
                    )
                );
        }

        @Test
        @DisplayName("Should NOT register RequestIdFilter when explicitly disabled")
        public void shouldNotRegisterRequestIdFilterWhenExplicitlyDisabled() {
            // Given: Property explicitly set to false

            // When / Then: ApplicationContext does not contain RequestIdFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-id.enabled=false")
                .run(
                    context -> assertThat(context.containsBean("requestIdFilter"), is(false))
                );
        }
    }

    // ======================== TRANSACTION-ID FILTER ========================


    @Nested
    @DisplayName("transaction-id filter")
    class TransactionIdFilter {

        @Test
        @DisplayName("Should NOT register TransactionIdFilter when property is absent (disabled by default)")
        public void shouldNotRegisterTransactionIdFilterWhenPropertyIsAbsent() {
            // Given: No property set (absent = OFF)

            // When / Then: ApplicationContext does not contain TransactionIdFilter bean
            contextRunner.run(
                context -> assertThat(context.containsBean("transactionIdFilter"), is(false))
            );
        }

        @Test
        @DisplayName("Should register TransactionIdFilter when explicitly enabled")
        public void shouldRegisterTransactionIdFilterWhenExplicitlyEnabled() {
            // Given: Property explicitly set to true

            // When / Then: ApplicationContext contains TransactionIdFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.transaction-id.enabled=true")
                .run(
                    context -> assertThat(
                        context.getBeansOfType(io.github.jframe.logging.filter.type.TransactionIdFilter.class).isEmpty(),
                        is(false)
                    )
                );
        }

        @Test
        @DisplayName("Should NOT register TransactionIdFilter when explicitly disabled")
        public void shouldNotRegisterTransactionIdFilterWhenExplicitlyDisabled() {
            // Given: Property explicitly set to false

            // When / Then: ApplicationContext does not contain TransactionIdFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.transaction-id.enabled=false")
                .run(
                    context -> assertThat(context.containsBean("transactionIdFilter"), is(false))
                );
        }
    }

    // ======================== REQUEST-DURATION FILTER ========================


    @Nested
    @DisplayName("request-duration filter")
    class RequestDurationFilter {

        @Test
        @DisplayName("Should register RequestDurationFilter when property is absent (ON by default)")
        public void shouldRegisterRequestDurationFilterWhenPropertyIsAbsent() {
            // Given: No property set (absent = ON)

            // When / Then: ApplicationContext contains RequestDurationFilter bean
            contextRunner.run(
                context -> assertThat(
                    context.getBeansOfType(io.github.jframe.logging.filter.type.RequestDurationFilter.class).isEmpty(),
                    is(false)
                )
            );
        }

        @Test
        @DisplayName("Should register RequestDurationFilter when explicitly enabled")
        public void shouldRegisterRequestDurationFilterWhenExplicitlyEnabled() {
            // Given: Property explicitly set to true

            // When / Then: ApplicationContext contains RequestDurationFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-duration.enabled=true")
                .run(
                    context -> assertThat(
                        context.getBeansOfType(io.github.jframe.logging.filter.type.RequestDurationFilter.class).isEmpty(),
                        is(false)
                    )
                );
        }

        @Test
        @DisplayName("Should NOT register RequestDurationFilter when explicitly disabled")
        public void shouldNotRegisterRequestDurationFilterWhenExplicitlyDisabled() {
            // Given: Property explicitly set to false

            // When / Then: ApplicationContext does not contain RequestDurationFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-duration.enabled=false")
                .run(
                    context -> assertThat(context.containsBean("requestDurationFilter"), is(false))
                );
        }
    }

    // ======================== REQUEST-RESPONSE FILTER ========================


    @Nested
    @DisplayName("request-response filter")
    class RequestResponseFilter {

        @Test
        @DisplayName("Should register RequestResponseLogFilter when property is absent (ON by default)")
        public void shouldRegisterRequestResponseLogFilterWhenPropertyIsAbsent() {
            // Given: No property set (absent = ON)

            // When / Then: ApplicationContext contains RequestResponseLogFilter bean
            contextRunner.run(
                context -> assertThat(
                    context.getBeansOfType(io.github.jframe.logging.filter.type.RequestResponseLogFilter.class).isEmpty(),
                    is(false)
                )
            );
        }

        @Test
        @DisplayName("Should register RequestResponseLogFilter when explicitly enabled")
        public void shouldRegisterRequestResponseLogFilterWhenExplicitlyEnabled() {
            // Given: Property explicitly set to true

            // When / Then: ApplicationContext contains RequestResponseLogFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-response.enabled=true")
                .run(
                    context -> assertThat(
                        context.getBeansOfType(io.github.jframe.logging.filter.type.RequestResponseLogFilter.class).isEmpty(),
                        is(false)
                    )
                );
        }

        @Test
        @DisplayName("Should NOT register RequestResponseLogFilter when explicitly disabled")
        public void shouldNotRegisterRequestResponseLogFilterWhenExplicitlyDisabled() {
            // Given: Property explicitly set to false

            // When / Then: ApplicationContext does not contain RequestResponseLogFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-response.enabled=false")
                .run(
                    context -> assertThat(context.containsBean("requestResponseLogFilter"), is(false))
                );
        }
    }
}
