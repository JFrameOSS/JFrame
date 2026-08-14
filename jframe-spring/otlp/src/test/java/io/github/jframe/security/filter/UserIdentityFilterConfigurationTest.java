package io.github.jframe.security.filter;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.runner.ApplicationContextRunner;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Context-runner tests for {@link UserIdentityFilterConfiguration} defaults.
 *
 * <p>Asserts that the user-identity filter bean is:
 * <ul>
 * <li><b>Present</b> when {@code jframe.logging.filters.user-identity.enabled} is absent (ON by default)</li>
 * <li><b>Present</b> when the property is explicitly {@code true}</li>
 * <li><b>Absent</b> when the property is explicitly {@code false}</li>
 * </ul>
 */
@DisplayName("Filter Configuration - UserIdentityFilterConfiguration defaults")
public class UserIdentityFilterConfigurationTest {

    private final ApplicationContextRunner contextRunner = new ApplicationContextRunner()
        .withUserConfiguration(UserIdentityFilterConfiguration.class);

    @Nested
    @DisplayName("user-identity filter")
    class UserIdentityFilter {

        @Test
        @DisplayName("Should register UserIdentityFilter when property is absent (ON by default)")
        public void shouldRegisterUserIdentityFilterWhenPropertyIsAbsent() {
            // Given: No property set (absent = ON)

            // When / Then: ApplicationContext contains UserIdentityFilter bean
            contextRunner.run(
                context -> assertThat(
                    context.getBeansOfType(io.github.jframe.security.filter.UserIdentityFilter.class).isEmpty(),
                    is(false)
                )
            );
        }

        @Test
        @DisplayName("Should register UserIdentityFilter when explicitly enabled")
        public void shouldRegisterUserIdentityFilterWhenExplicitlyEnabled() {
            // Given: Property explicitly set to true

            // When / Then: ApplicationContext contains UserIdentityFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.user-identity.enabled=true")
                .run(
                    context -> assertThat(
                        context.getBeansOfType(io.github.jframe.security.filter.UserIdentityFilter.class).isEmpty(),
                        is(false)
                    )
                );
        }

        @Test
        @DisplayName("Should NOT register UserIdentityFilter when explicitly disabled")
        public void shouldNotRegisterUserIdentityFilterWhenExplicitlyDisabled() {
            // Given: Property explicitly set to false

            // When / Then: ApplicationContext does not contain UserIdentityFilter bean
            contextRunner
                .withPropertyValues("jframe.logging.filters.user-identity.enabled=false")
                .run(
                    context -> assertThat(context.containsBean("userIdentityFilter"), is(false))
                );
        }
    }
}
