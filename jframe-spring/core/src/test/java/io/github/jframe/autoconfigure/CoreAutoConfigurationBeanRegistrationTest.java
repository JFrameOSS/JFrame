package io.github.jframe.autoconfigure;

import io.github.jframe.autoconfigure.properties.ApplicationProperties;
import io.github.jframe.autoconfigure.properties.LoggingProperties;
import io.github.jframe.exception.factory.DefaultExceptionResponseFactory;
import io.github.jframe.exception.factory.ErrorResponseEntityBuilder;
import io.github.jframe.exception.handler.JFrameResponseEntityExceptionHandler;
import io.github.jframe.exception.handler.enricher.ErrorCodeResponseEnricher;
import io.github.jframe.exception.handler.enricher.MethodArgumentNotValidResponseEnricher;
import io.github.jframe.exception.handler.enricher.RateLimitResponseEnricher;
import io.github.jframe.exception.handler.enricher.RequestInfoResponseEnricher;
import io.github.jframe.exception.handler.enricher.StatusCodeResponseEnricher;
import io.github.jframe.exception.handler.enricher.TransactionIdResponseEnricher;
import io.github.jframe.exception.handler.enricher.ValidationErrorResponseEnricher;
import io.github.jframe.exception.resource.ObjectErrorResourceAssembler;
import io.github.jframe.exception.resource.ValidationErrorResourceAssembler;
import io.github.jframe.logging.filter.FilterConfiguration;
import io.github.jframe.logging.filter.config.RequestDurationFilterConfiguration;
import io.github.jframe.logging.filter.config.RequestIdFilterConfiguration;
import io.github.jframe.logging.filter.config.RequestResponseLogFilterConfiguration;
import io.github.jframe.logging.filter.config.TransactionIdFilterConfiguration;
import io.github.jframe.logging.interceptor.LoggingClientHttpRequestInterceptor;
import io.github.jframe.logging.logger.HttpRequestResponseBodyLogger;
import io.github.jframe.logging.logger.HttpRequestResponseDebugLogger;
import io.github.jframe.logging.logger.HttpRequestResponseHeadersLogger;
import io.github.jframe.logging.masker.type.PasswordMasker;
import io.github.jframe.logging.scheduled.ScheduledAspect;
import io.github.jframe.logging.voter.FilterVoter;
import io.github.jframe.logging.voter.MediaTypeVoter;
import io.github.jframe.logging.voter.RequestVoter;
import io.github.jframe.util.mapper.DateTimeMapper;
import tools.jackson.databind.ObjectMapper;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.autoconfigure.AutoConfigurations;
import org.springframework.boot.test.context.runner.WebApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;
import static org.hamcrest.Matchers.notNullValue;

/**
 * Characterization / regression test for {@link CoreAutoConfiguration} bean registration surface.
 *
 * <p>Purpose: jframe is a published library. {@link CoreAutoConfiguration} currently carries
 * {@code @ComponentScan(basePackages = "io.github.jframe")}. We are about to DELETE that scan
 * and replace it with explicit {@code @Import} / {@code @Bean} registration. This suite is the
 * safety net that proves no bean silently disappears in that refactor.
 *
 * <p>Tests assert the OBSERVABLE OUTCOME (bean is in the context), not the mechanism
 * ({@code @ComponentScan} vs {@code @Import}). All tests MUST PASS against the unmodified code.
 * Any assertion failure today is a finding to report, not to work around.
 */
@DisplayName("Characterization Test - CoreAutoConfiguration bean registration surface")
class CoreAutoConfigurationBeanRegistrationTest {

    private static final String APP_NAME = "jframe.application.name=test-service";
    private static final String APP_GROUP = "jframe.application.group=io.github.jframe";
    private static final String APP_VERSION = "jframe.application.version=0.0.1";

    /** Minimal properties required for the context to start. */
    private final WebApplicationContextRunner contextRunner = new WebApplicationContextRunner()
        .withConfiguration(AutoConfigurations.of(CoreAutoConfiguration.class))
        .withBean(ObjectMapper.class, ObjectMapper::new)
        .withPropertyValues(APP_NAME, APP_GROUP, APP_VERSION);

    // =========================================================================
    // Default context — no extra property overrides
    // =========================================================================

    @Nested
    @DisplayName("Default context (no property overrides)")
    class DefaultContext {

        @Test
        @DisplayName("Should register JacksonConfig bean")
        void shouldRegisterJacksonConfig() {
            // Given: default CoreAutoConfiguration context
            contextRunner.run(ctx -> {
                // When / Then: JacksonConfig must be present
                assertThat(
                    "JacksonConfig must be registered by CoreAutoConfiguration",
                    ctx.getBean(JacksonConfig.class),
                    is(notNullValue())
                );
            });
        }

        @Test
        @DisplayName("Should register CorePackageLogger bean")
        void shouldRegisterCorePackageLogger() {
            contextRunner.run(
                ctx -> assertThat(
                    "CorePackageLogger must be registered",
                    ctx.getBean(CorePackageLogger.class),
                    is(notNullValue())
                )
            );
        }

        @Test
        @DisplayName("Should register all exception enricher beans")
        void shouldRegisterAllExceptionEnricherBeans() {
            // Given: default CoreAutoConfiguration context
            contextRunner.run(ctx -> {
                // When / Then: every enricher picked up by component scan must survive the refactor
                assertThat("RateLimitResponseEnricher", ctx.getBean(RateLimitResponseEnricher.class), is(notNullValue()));
                assertThat("StatusCodeResponseEnricher", ctx.getBean(StatusCodeResponseEnricher.class), is(notNullValue()));
                assertThat("RequestInfoResponseEnricher", ctx.getBean(RequestInfoResponseEnricher.class), is(notNullValue()));
                // TransactionIdResponseEnricher is @ConditionalOnProperty(transaction-id.enabled) and
                // jframe-properties.yml ships transaction-id.enabled=false, so it is ABSENT by default.
                // It is asserted in the ConditionallyPresentWhenEnabled suite below.
                assertThat("ErrorCodeResponseEnricher", ctx.getBean(ErrorCodeResponseEnricher.class), is(notNullValue()));
                assertThat(
                    "MethodArgumentNotValidResponseEnricher",
                    ctx.getBean(MethodArgumentNotValidResponseEnricher.class),
                    is(notNullValue())
                );
                assertThat("ValidationErrorResponseEnricher", ctx.getBean(ValidationErrorResponseEnricher.class), is(notNullValue()));
            });
        }

        @Test
        @DisplayName("Should register exception handler infrastructure beans")
        void shouldRegisterExceptionHandlerInfrastructureBeans() {
            contextRunner.run(ctx -> {
                assertThat(
                    "JFrameResponseEntityExceptionHandler",
                    ctx.getBean(JFrameResponseEntityExceptionHandler.class),
                    is(notNullValue())
                );
                assertThat("ObjectErrorResourceAssembler", ctx.getBean(ObjectErrorResourceAssembler.class), is(notNullValue()));
                assertThat("ValidationErrorResourceAssembler", ctx.getBean(ValidationErrorResourceAssembler.class), is(notNullValue()));
                assertThat("ErrorResponseEntityBuilder", ctx.getBean(ErrorResponseEntityBuilder.class), is(notNullValue()));
                assertThat("DefaultExceptionResponseFactory", ctx.getBean(DefaultExceptionResponseFactory.class), is(notNullValue()));
            });
        }

        @Test
        @DisplayName("Should register logging logger beans")
        void shouldRegisterLoggingLoggerBeans() {
            contextRunner.run(ctx -> {
                assertThat("HttpRequestResponseBodyLogger", ctx.getBean(HttpRequestResponseBodyLogger.class), is(notNullValue()));
                assertThat("HttpRequestResponseHeadersLogger", ctx.getBean(HttpRequestResponseHeadersLogger.class), is(notNullValue()));
                assertThat("HttpRequestResponseDebugLogger", ctx.getBean(HttpRequestResponseDebugLogger.class), is(notNullValue()));
                assertThat(
                    "LoggingClientHttpRequestInterceptor",
                    ctx.getBean(LoggingClientHttpRequestInterceptor.class),
                    is(notNullValue())
                );
                assertThat("ScheduledAspect", ctx.getBean(ScheduledAspect.class), is(notNullValue()));
            });
        }

        @Test
        @DisplayName("Should register logging voter beans declared directly in CoreAutoConfiguration")
        void shouldRegisterLoggingVoterBeans() {
            contextRunner.run(ctx -> {
                assertThat("RequestVoter", ctx.getBean(RequestVoter.class), is(notNullValue()));
                assertThat("FilterVoter", ctx.getBean(FilterVoter.class), is(notNullValue()));
                assertThat("PasswordMasker", ctx.getBean(PasswordMasker.class), is(notNullValue()));
                // Two MediaTypeVoter beans — assert both exist by the two known qualifier names
                assertThat("mediaTypeVoter bean", ctx.getBean("mediaTypeVoter", MediaTypeVoter.class), is(notNullValue()));
                assertThat(
                    "bodyExcludedMediaTypeVoter bean",
                    ctx.getBean("bodyExcludedMediaTypeVoter", MediaTypeVoter.class),
                    is(notNullValue())
                );
            });
        }

        @Test
        @DisplayName("Should register FilterConfiguration and enabled-by-default filter configs")
        void shouldRegisterFilterConfigurationBeans() {
            // Given: default properties — request-response ON, request-duration ON
            contextRunner.run(ctx -> {
                assertThat("FilterConfiguration", ctx.getBean(FilterConfiguration.class), is(notNullValue()));
                assertThat(
                    "RequestResponseLogFilterConfiguration (default: enabled=true)",
                    ctx.getBean(RequestResponseLogFilterConfiguration.class),
                    is(notNullValue())
                );
                assertThat(
                    "RequestDurationFilterConfiguration (default: enabled=true)",
                    ctx.getBean(RequestDurationFilterConfiguration.class),
                    is(notNullValue())
                );
            });
        }

        @Test
        @DisplayName("Should register property-binding beans (ApplicationProperties, LoggingProperties)")
        void shouldRegisterConfigurationPropertiesBeans() {
            contextRunner.run(ctx -> {
                assertThat("ApplicationProperties", ctx.getBean(ApplicationProperties.class), is(notNullValue()));
                assertThat("LoggingProperties", ctx.getBean(LoggingProperties.class), is(notNullValue()));
            });
        }

        /**
         * Asserts that the MapStruct-generated {@link DateTimeMapper} implementation is present.
         * The generated class lives in {@code io.github.jframe.util.mapper} and is currently
         * picked up by {@code @ComponentScan}. If the scan is removed, the refactor MUST include
         * an explicit registration for this generated bean.
         */
        @Test
        @DisplayName("Should register DateTimeMapper bean (MapStruct-generated, must survive scan removal)")
        void shouldRegisterDateTimeMapperBean() {
            contextRunner.run(
                ctx -> assertThat(
                    "DateTimeMapper (MapStruct-generated DateTimeMapperImpl) must be present in context",
                    ctx.getBean(DateTimeMapper.class),
                    is(notNullValue())
                )
            );
        }
    }

    // =========================================================================
    // Conditional beans — ABSENT by default
    // =========================================================================


    @Nested
    @DisplayName("Conditionally absent beans (default property values disable them)")
    class ConditionallyAbsentByDefault {

        /**
         * {@link RequestIdFilterConfiguration} is gated on
         * {@code jframe.logging.filters.request-id.enabled=true} with {@code matchIfMissing=false}.
         * The shipped {@code jframe-properties.yml} sets {@code enabled: false}, so the bean
         * is ABSENT from a default context.
         */
        @Test
        @DisplayName("Should NOT register RequestIdFilterConfiguration when request-id filter is disabled (default)")
        void shouldNotRegisterRequestIdFilterConfigurationByDefault() {
            // Given: default context — jframe-properties.yml ships request-id.enabled=false
            contextRunner.run(ctx ->
            // Then: the configuration class itself must not be present
            assertThat(
                "RequestIdFilterConfiguration must be absent when request-id.enabled=false (the default)",
                ctx.containsBean("requestIdFilterConfiguration"),
                is(false)
            )
            );
        }

        /**
         * {@link TransactionIdFilterConfiguration} is gated on
         * {@code jframe.logging.filters.transaction-id.enabled=true} with {@code matchIfMissing=false}.
         * The shipped {@code jframe-properties.yml} sets {@code enabled: false}, so the bean
         * is ABSENT from a default context.
         */
        @Test
        @DisplayName("Should NOT register TransactionIdFilterConfiguration when transaction-id filter is disabled (default)")
        void shouldNotRegisterTransactionIdFilterConfigurationByDefault() {
            // Given: default context — jframe-properties.yml ships transaction-id.enabled=false
            contextRunner.run(
                ctx -> assertThat(
                    "TransactionIdFilterConfiguration must be absent when transaction-id.enabled=false (the default)",
                    ctx.containsBean("transactionIdFilterConfiguration"),
                    is(false)
                )
            );
        }
    }

    // =========================================================================
    // Conditional beans — explicitly enabled
    // =========================================================================


    @Nested
    @DisplayName("Conditionally present beans (explicitly enabled via property override)")
    class ConditionallyPresentWhenEnabled {

        @Test
        @DisplayName("Should register RequestIdFilterConfiguration when request-id filter explicitly enabled")
        void shouldRegisterRequestIdFilterConfigurationWhenEnabled() {
            // Given: request-id filter explicitly enabled
            contextRunner
                .withPropertyValues("jframe.logging.filters.request-id.enabled=true")
                .run(
                    ctx -> assertThat(
                        "RequestIdFilterConfiguration must be present when request-id.enabled=true",
                        ctx.getBean(RequestIdFilterConfiguration.class),
                        is(notNullValue())
                    )
                );
        }

        @Test
        @DisplayName("Should register TransactionIdFilterConfiguration when transaction-id filter explicitly enabled")
        void shouldRegisterTransactionIdFilterConfigurationWhenEnabled() {
            // Given: transaction-id filter explicitly enabled
            contextRunner
                .withPropertyValues("jframe.logging.filters.transaction-id.enabled=true")
                .run(
                    ctx -> assertThat(
                        "TransactionIdFilterConfiguration must be present when transaction-id.enabled=true",
                        ctx.getBean(TransactionIdFilterConfiguration.class),
                        is(notNullValue())
                    )
                );
        }

        @Test
        @DisplayName("Should register TransactionIdResponseEnricher when transaction-id filter explicitly enabled")
        void shouldRegisterTransactionIdResponseEnricherWhenEnabled() {
            // Given: transaction-id filter enabled — TransactionIdResponseEnricher shares the same condition
            contextRunner
                .withPropertyValues("jframe.logging.filters.transaction-id.enabled=true")
                .run(
                    ctx -> assertThat(
                        "TransactionIdResponseEnricher must be present when transaction-id.enabled=true",
                        ctx.getBean(TransactionIdResponseEnricher.class),
                        is(notNullValue())
                    )
                );
        }
    }
}
