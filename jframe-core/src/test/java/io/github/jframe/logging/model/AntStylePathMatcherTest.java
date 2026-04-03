package io.github.jframe.logging.model;

import io.github.support.UnitTest;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Tests for {@link AntStylePathMatcher}.
 *
 * <p>Verifies the AntStylePathMatcher functionality including:
 * <ul>
 * <li>Exact path matching</li>
 * <li>Single wildcard (*) — matches a single path segment</li>
 * <li>Double wildcard (**) — matches zero or more path segments</li>
 * <li>Question mark (?) — matches a single character</li>
 * <li>Complex composite patterns</li>
 * <li>No-match cases</li>
 * <li>Null / empty input handling</li>
 * </ul>
 */
@DisplayName("Logging - AntStylePathMatcher")
public class AntStylePathMatcherTest extends UnitTest {

    // ---- Exact matching --------------------------------------------------

    @Test
    @DisplayName("Should match exact path")
    public void shouldMatchExactPath() {
        // Given: An exact pattern and matching path
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/users");

        // When: Testing the matching path
        final boolean result = matcher.matches("/api/users");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match different exact path")
    public void shouldNotMatchDifferentExactPath() {
        // Given: An exact pattern and a non-matching path
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/users");

        // When: Testing a different path
        final boolean result = matcher.matches("/api/orders");

        // Then: No match
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should match root path exactly")
    public void shouldMatchRootPath() {
        // Given: Root pattern
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/");

        // When: Testing root path
        final boolean result = matcher.matches("/");

        // Then: Match is found
        assertThat(result, is(true));
    }

    // ---- Single wildcard (*) ---------------------------------------------

    @Test
    @DisplayName("Should match single segment with * wildcard")
    public void shouldMatchSingleSegmentWithWildcard() {
        // Given: A pattern with single wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*");

        // When: Testing path with single trailing segment
        final boolean result = matcher.matches("/api/users");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match multiple segments with single * wildcard")
    public void shouldNotMatchMultipleSegmentsWithSingleWildcard() {
        // Given: A pattern with single wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*");

        // When: Testing path with multiple trailing segments
        final boolean result = matcher.matches("/api/users/1");

        // Then: No match — * does not cross path separators
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should match any single segment with mid-path * wildcard")
    public void shouldMatchAnySingleSegmentMidPath() {
        // Given: A pattern with wildcard in the middle
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*/profile");

        // When: Testing path with any user id
        final boolean result = matcher.matches("/api/123/profile");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match empty segment with * wildcard when segment required")
    public void shouldNotMatchEmptySegmentWithWildcard() {
        // Given: A pattern expecting a segment
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*/profile");

        // When: Testing path with empty segment (double slash)
        final boolean result = matcher.matches("/api//profile");

        // Then: No match
        assertThat(result, is(false));
    }

    // ---- Double wildcard (**) -------------------------------------------

    @Test
    @DisplayName("Should match single segment with ** wildcard")
    public void shouldMatchSingleSegmentWithDoubleWildcard() {
        // Given: A pattern with double wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/**");

        // When: Testing path with single trailing segment
        final boolean result = matcher.matches("/api/users");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should match multiple segments with ** wildcard")
    public void shouldMatchMultipleSegmentsWithDoubleWildcard() {
        // Given: A pattern with double wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/**");

        // When: Testing path with multiple segments
        final boolean result = matcher.matches("/api/users/1");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should match deeply nested path with ** wildcard")
    public void shouldMatchDeeplyNestedPathWithDoubleWildcard() {
        // Given: A pattern with double wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/**");

        // When: Testing deeply nested path
        final boolean result = matcher.matches("/api/users/1/profile");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match path that does not start with pattern prefix when using **")
    public void shouldNotMatchPathWithDifferentPrefix() {
        // Given: A pattern with prefix and double wildcard
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/**");

        // When: Testing path with different prefix
        final boolean result = matcher.matches("/other/users/1");

        // Then: No match
        assertThat(result, is(false));
    }

    // ---- Question mark (?) single char ----------------------------------

    @Test
    @DisplayName("Should match single character with ? wildcard")
    public void shouldMatchSingleCharWithQuestionMark() {
        // Given: A pattern with question mark
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/user?");

        // When: Testing path that adds exactly one character
        final boolean result = matcher.matches("/api/users");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match empty replacement for ? wildcard")
    public void shouldNotMatchEmptyReplacementForQuestionMark() {
        // Given: A pattern with question mark
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/user?");

        // When: Testing the path without the extra character
        final boolean result = matcher.matches("/api/user");

        // Then: No match — ? requires exactly one character
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should not match two characters with single ? wildcard")
    public void shouldNotMatchTwoCharsWithSingleQuestionMark() {
        // Given: A pattern with question mark
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/user?");

        // When: Testing path with two extra characters
        final boolean result = matcher.matches("/api/userab");

        // Then: No match — ? matches exactly one character
        assertThat(result, is(false));
    }

    // ---- Complex patterns -----------------------------------------------

    @Test
    @DisplayName("Should match complex pattern /api/*/profile/**")
    public void shouldMatchComplexPatternApiProfileDeep() {
        // Given: A complex pattern
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*/profile/**");

        // When: Testing a matching path
        final boolean result = matcher.matches("/api/123/profile/settings/notifications");

        // Then: Match is found
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should not match complex pattern /api/*/profile/** when profile segment missing")
    public void shouldNotMatchComplexPatternWhenProfileMissing() {
        // Given: A complex pattern
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/*/profile/**");

        // When: Testing a path without the profile segment
        final boolean result = matcher.matches("/api/123/settings/notifications");

        // Then: No match
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should match health endpoint pattern")
    public void shouldMatchHealthEndpointPattern() {
        // Given: A common actuator exclude pattern
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/actuator/**");

        // When: Testing various actuator paths
        final boolean healthResult = matcher.matches("/actuator/health");
        final boolean metricsResult = matcher.matches("/actuator/metrics/jvm.memory.used");

        // Then: Both match
        assertThat(healthResult, is(true));
        assertThat(metricsResult, is(true));
    }

    // ---- Null / empty input handling ------------------------------------

    @Test
    @DisplayName("Should throw NullPointerException or return false for null path")
    public void shouldHandleNullPath() {
        // Given: A valid pattern and null path
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/**");

        // When: Testing null path
        // Then: Either throws NPE or returns false (implementation-defined)
        try {
            final boolean result = matcher.matches(null);
            assertThat(result, is(false));
        } catch (final NullPointerException expected) {
            // acceptable behaviour
        }
    }

    @Test
    @DisplayName("Should not match empty path against non-empty pattern")
    public void shouldNotMatchEmptyPathAgainstNonEmptyPattern() {
        // Given: A non-empty pattern and empty path
        final AntStylePathMatcher matcher = new AntStylePathMatcher("/api/users");

        // When: Testing empty path
        final boolean result = matcher.matches("");

        // Then: No match
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should throw NullPointerException or fail fast for null pattern")
    public void shouldHandleNullPattern() {
        // Given: A null pattern

        // When: Constructing matcher with null
        // Then: Either throws NPE at construction time, or false on match
        try {
            final AntStylePathMatcher matcher = new AntStylePathMatcher(null);
            final boolean result = matcher.matches("/api/users");
            assertThat(result, is(false));
        } catch (final NullPointerException expected) {
            // acceptable — fail-fast construction
        }
    }

    @Test
    @DisplayName("Should match empty pattern against empty path")
    public void shouldMatchEmptyPatternAgainstEmptyPath() {
        // Given: Empty pattern and empty path
        final AntStylePathMatcher matcher = new AntStylePathMatcher("");

        // When: Testing empty path
        final boolean result = matcher.matches("");

        // Then: Exact empty match
        assertThat(result, is(true));
    }
}
