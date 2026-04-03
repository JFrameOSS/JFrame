package io.github.jframe.logging.voter;

import io.github.jframe.logging.model.PathDefinition;
import io.github.support.UnitTest;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import static org.hamcrest.MatcherAssert.assertThat;
import static org.hamcrest.Matchers.is;

/**
 * Unit tests for {@link RequestVoter}.
 *
 * <p>Verifies RequestVoter decision logic for path-based request exclusions including:
 * <ul>
 * <li>Null / empty exclusion list always allows the request</li>
 * <li>Exact path matching excludes the request</li>
 * <li>Ant-style wildcard path matching (*, **)</li>
 * <li>Method-restricted exclusions</li>
 * <li>Multiple exclusion patterns evaluated in order</li>
 * </ul>
 */
@DisplayName("Quarkus Logging Voters - RequestVoter")
public class RequestVoterTest extends UnitTest {

    // ---------------------------------------------------------------------------
    // Null / empty exclusion list
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when excludePaths list is null")
    public void shouldReturnTrueWhenExcludePathsListIsNull() {
        // Given: A voter with null exclusion list
        final RequestVoter voter = new RequestVoter(null);

        // When: Checking any request
        final boolean result = voter.allowed("GET", "/api/users");

        // Then: Request is allowed because there are no exclusions
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true when excludePaths list is empty")
    public void shouldReturnTrueWhenExcludePathsListIsEmpty() {
        // Given: A voter with empty exclusion list
        final RequestVoter voter = new RequestVoter(Collections.emptyList());

        // When: Checking any request
        final boolean result = voter.allowed("GET", "/api/users");

        // Then: Request is allowed because there are no exclusions
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // No match — request is allowed
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return true when path does not match any exclusion pattern")
    public void shouldReturnTrueWhenPathDoesNotMatchAnyExclusionPattern() {
        // Given: A voter excluding /actuator/* paths
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("/actuator/*"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a non-matching path
        final boolean result = voter.allowed("GET", "/api/users");

        // Then: Request is allowed because path does not match the exclusion
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // Wildcard path matching — request is excluded
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when path matches single-level wildcard exclusion")
    public void shouldReturnFalseWhenPathMatchesSingleLevelWildcardExclusion() {
        // Given: A voter excluding /actuator/* paths
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("/actuator/*"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a path that matches the wildcard
        final boolean result = voter.allowed("GET", "/actuator/health");

        // Then: Request is excluded because path matches /actuator/*
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return false when path matches double-wildcard exclusion")
    public void shouldReturnFalseWhenPathMatchesDoubleWildcardExclusion() {
        // Given: A voter excluding all paths under /admin/**
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("/admin/**"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a nested path
        final boolean result = voter.allowed("GET", "/admin/users/123");

        // Then: Request is excluded because path matches /admin/**
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true when path does not match double-wildcard exclusion")
    public void shouldReturnTrueWhenPathDoesNotMatchDoubleWildcardExclusion() {
        // Given: A voter excluding all paths under /admin/**
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("/admin/**"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a path outside /admin
        final boolean result = voter.allowed("GET", "/api/users/123");

        // Then: Request is allowed because path does not match /admin/**
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // Method-restricted exclusions
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when method and path both match method-restricted exclusion")
    public void shouldReturnFalseWhenMethodAndPathBothMatchMethodRestrictedExclusion() {
        // Given: A voter excluding only GET /health
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("GET", "/health"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking GET /health
        final boolean result = voter.allowed("GET", "/health");

        // Then: Request is excluded because method and path both match
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true when method does not match method-restricted exclusion")
    public void shouldReturnTrueWhenMethodDoesNotMatchMethodRestrictedExclusion() {
        // Given: A voter excluding only GET /health
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("GET", "/health"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking POST /health (different method)
        final boolean result = voter.allowed("POST", "/health");

        // Then: Request is allowed because method does not match the exclusion
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return true when path does not match method-restricted exclusion even if method matches")
    public void shouldReturnTrueWhenPathDoesNotMatchMethodRestrictedExclusionEvenIfMethodMatches() {
        // Given: A voter excluding only GET /health
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("GET", "/health"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking GET /other-path
        final boolean result = voter.allowed("GET", "/other-path");

        // Then: Request is allowed because path does not match even though method matches
        assertThat(result, is(true));
    }

    // ---------------------------------------------------------------------------
    // Multiple exclusions
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when path matches second exclusion pattern in list")
    public void shouldReturnFalseWhenPathMatchesSecondExclusionPatternInList() {
        // Given: A voter with multiple exclusion patterns
        final List<PathDefinition> exclusions = Arrays.asList(
            new PathDefinition("/actuator/*"),
            new PathDefinition("/health")
        );
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a path that matches the second exclusion
        final boolean result = voter.allowed("GET", "/health");

        // Then: Request is excluded because it matches /health
        assertThat(result, is(false));
    }

    @Test
    @DisplayName("Should return true when path matches none of multiple exclusion patterns")
    public void shouldReturnTrueWhenPathMatchesNoneOfMultipleExclusionPatterns() {
        // Given: A voter with multiple exclusion patterns
        final List<PathDefinition> exclusions = Arrays.asList(
            new PathDefinition("/actuator/*"),
            new PathDefinition("/health")
        );
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a path that matches no exclusion
        final boolean result = voter.allowed("GET", "/api/orders");

        // Then: Request is allowed because no exclusion matches
        assertThat(result, is(true));
    }

    @Test
    @DisplayName("Should return false when path matches first of multiple exclusion patterns")
    public void shouldReturnFalseWhenPathMatchesFirstOfMultipleExclusionPatterns() {
        // Given: A voter with multiple exclusion patterns
        final List<PathDefinition> exclusions = Arrays.asList(
            new PathDefinition("/actuator/*"),
            new PathDefinition("/health")
        );
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking a path matching the first pattern
        final boolean result = voter.allowed("GET", "/actuator/health");

        // Then: Request is excluded because it matches /actuator/*
        assertThat(result, is(false));
    }

    // ---------------------------------------------------------------------------
    // Exact path matching
    // ---------------------------------------------------------------------------

    @Test
    @DisplayName("Should return false when path exactly matches exclusion pattern")
    public void shouldReturnFalseWhenPathExactlyMatchesExclusionPattern() {
        // Given: A voter with an exact path exclusion
        final List<PathDefinition> exclusions = Collections.singletonList(new PathDefinition("/ping"));
        final RequestVoter voter = new RequestVoter(exclusions);

        // When: Checking the exact path
        final boolean result = voter.allowed("GET", "/ping");

        // Then: Request is excluded because path matches exactly
        assertThat(result, is(false));
    }
}
